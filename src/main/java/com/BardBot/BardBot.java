package com.BardBot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Color;
import discord4j.voice.AudioProvider;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Duration;
import java.util.*;

public class BardBot {

    private static final Map<String,Command> commands = new HashMap<>();

    public static void main (String[] args){

        Gson gson = new Gson();
        JsonObject jsObj;

        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/java/com/BardBot/data.json"));
            jsObj = gson.fromJson(br, JsonObject.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        String prefix = jsObj.get("Prefix").getAsString();

        YoutubeAudioSourceManager youtube = new dev.lavalink.youtube.YoutubeAudioSourceManager(false);
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

        if(jsObj.get("RefreshToken").getAsString().isEmpty()){
            youtube.useOauth2(null,false);
        }else{
            youtube.useOauth2(jsObj.get("RefreshToken").getAsString(),true);
        }

        playerManager.registerSourceManager(youtube);
        AudioSourceManagers.registerRemoteSources(playerManager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);

        Map<Optional<Snowflake>, TrackScheduler> TrackSchedulers = new HashMap<>();

        commands.put("help", event ->
                event.getMessage().getChannel().subscribe(messageChannel -> messageChannel.createMessage(Embeds.helpEmbed(prefix)).block()));

        commands.put("play", event -> {
            if(event.getMessage().getContent().length() > 6) {
                validateNPlay(event,TrackSchedulers,playerManager,null);

            }else {
                if (!event.getMessage().getAttachments().isEmpty()) {
                    if(event.getMessage().getAttachments().size() > 1) {
                        Attachment attachments = event.getMessage().getAttachments().get(0);

                        if (attachments.getContentType().isPresent()) {
                            if (attachments.getContentType().get().equals("audio/mpeg") || attachments.getContentType().get().equals("video/mp4")) {

                                validateNPlay(event, TrackSchedulers, playerManager, attachments);

                            } else {
                                sendMessage(event, "This file extension is not supported! Use mp4 or mp3");
                            }
                        }
                    }else {
                        sendMessage(event, "If you want bot to play from an attachment send only one!");
                    }
                } else {
                    sendMessage(event, "You need to type a song!");
                }
            }
                event.getMessage().delete().block();
            });

            commands.put("queue", event -> {
                if(TrackSchedulers.containsKey(event.getGuildId())){
                    if(TrackSchedulers.get(event.getGuildId()).queue.isEmpty()){
                        event.getMessage().getChannel().subscribe(messageChannel -> messageChannel.createMessage(Embeds.embed.withDescription("Queue is empty!")).block());
                        return;
                    }

                    event.getMessage().getChannel().subscribe(messageChannel -> messageChannel.createEmbed(legacyEmbedCreateSpec -> {
                        List<AudioTrack> tracks = TrackSchedulers.get(event.getGuildId()).queue;
                        legacyEmbedCreateSpec.addField("Now playing: " + tracks.get(0).getInfo().title, tracks.get(0).getInfo().uri, false);

                         for(int i = 1; i < tracks.size(); i++){

                             if(i > 23){
                                 legacyEmbedCreateSpec.addField("+" + (tracks.size() - 24), "more titles", false);
                                 break;
                             }
                             legacyEmbedCreateSpec.addField(i + ". " + tracks.get(i).getInfo().title, tracks.get(i).getInfo().uri, false);
                             legacyEmbedCreateSpec.setColor(Color.RED);
                         }
                    }).block());

                }else {
                    sendMessage(event, "There is no queue");
                }
            });

            commands.put("status", event -> {
                event.getMessage().delete().block();

                if(TrackSchedulers.containsKey(event.getGuildId())){
                    if(TrackSchedulers.get(event.getGuildId()).player.getPlayingTrack() == null){
                        sendMessage(event, "Bot is currently not playing anything");
                    }else{
                        AudioTrack track = TrackSchedulers.get(event.getGuildId()).player.getPlayingTrack();
                        event.getMessage().getChannel().subscribe(channel -> channel.createMessage(Embeds.statusEmbed(track)).block());
                    }
                }else{
                    sendMessage(event, "Play something first!");
                }
            });

            commands.put("skip", event -> {
                event.getMessage().delete().block();

                if(TrackSchedulers.containsKey(event.getGuildId())){
                    if(TrackSchedulers.get(event.getGuildId()).player.getPlayingTrack() == null){
                        sendMessage(event, "Bot is currently not playing anything");
                    }else{
                        TrackSchedulers.get(event.getGuildId()).skip();
                    }
                }else{
                    sendMessage(event, "Play something first!");
                }
            });

            commands.put("clear", event -> {
                event.getMessage().delete().block();

                if(TrackSchedulers.containsKey(event.getGuildId())){
                    if(TrackSchedulers.get(event.getGuildId()).queue.isEmpty()){
                        sendMessage(event, "Queue is already empty!");
                    }else{
                        TrackSchedulers.get(event.getGuildId()).clearQueue();
                        sendMessage(event, "Queue cleared successfully!");
                    }
                }else {
                    sendMessage(event, "Play something first");
                }
            });

            commands.put("loop", event -> {
                event.getMessage().delete().block();

                if(TrackSchedulers.containsKey(event.getGuildId())){
                    if(event.getMessage().getContent().length() < 6){
                        sendMessage(event, "Improper use of loop command, check -help for proper use");
                        return;
                    }
                    String command = Arrays.asList(event.getMessage().getContent().toLowerCase().substring(6).split(" ")).get(0);

                    switch (command) {
                        case "off" -> {
                            TrackSchedulers.get(event.getGuildId()).changeLoop(0);
                            sendMessage(event, "Looping is now off");
                        }
                        case "single" -> {
                            TrackSchedulers.get(event.getGuildId()).changeLoop(1);
                            sendMessage(event, "Loop is now set for looping current track");
                        }
                        case "all" -> {
                            TrackSchedulers.get(event.getGuildId()).changeLoop(2);
                            sendMessage(event, "Loop is now set for looping queue");
                        }
                    }
                }else{
                    sendMessage(event, "Bot is not connected to a voice channel!");
                }
            });

        GatewayDiscordClient client = DiscordClientBuilder.create(jsObj.get("BotToken").getAsString()).build().login().block();
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(readyEvent -> System.out.println(readyEvent.getSelf().getUsername() + " is online"));

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    final String content = event.getMessage().getContent().toLowerCase(Locale.ROOT);
                        for (final Map.Entry<String,Command> entry : commands.entrySet()){
                            if(content.startsWith(prefix + entry.getKey())){
                                entry.getValue().execute(event);
                                break;
                            }
                    }
                });

        client.onDisconnect().block();
    }

    private static void sendMessage(MessageCreateEvent event, String description){
        event.getMessage().getChannel().subscribe(channel -> channel.createMessage((Embeds.embed).withDescription(description)).block());
    }

    private static void validateNPlay(MessageCreateEvent event,Map<Optional<Snowflake>, TrackScheduler> TrackSchedulers, AudioPlayerManager playerManager, Attachment attachment) {

        final Member member = event.getMember().orElse(null);
        final List<String> command;

        if(attachment == null){
            command = Arrays.asList(event.getMessage().getContent().substring(6).split(" "));
        }else{
            command = null;
        }

        if (member != null) {
            final VoiceState voiceState = member.getVoiceState().block();
            if (voiceState != null) {
                final VoiceChannel voiceChannel = voiceState.getChannel().block();
                if (voiceChannel != null) {

                    if (!TrackSchedulers.containsKey(event.getGuildId())) {
                        AudioPlayer player = playerManager.createPlayer();
                        AudioProvider provider = new LavaPlayerAudioProvider(player);
                        TrackScheduler trackScheduler = new TrackScheduler(player, command, provider, event.getMessage().getChannel().block(), playerManager);
                        player.addListener(trackScheduler);
                        voiceChannel.join(channelJoinSpec -> channelJoinSpec.setProvider(provider)).subscribe(trackScheduler::setVoiceConnection);
                        playerManager.loadItem((attachment != null? attachment.getUrl(): command).toString(), trackScheduler);
                        TrackSchedulers.put(event.getGuildId(), trackScheduler);

                    } else {
                        voiceChannel.join(channelJoinSpec -> channelJoinSpec.setProvider(TrackSchedulers.get(event.getGuildId()).provider)).subscribe(voiceConnection -> TrackSchedulers.get(event.getGuildId()).setVoiceConnection(voiceConnection));
                        if(command != null )TrackSchedulers.get(event.getGuildId()).setCommand(command);
                        Mono.delay(Duration.ofSeconds(1L));
                        playerManager.loadItem((attachment != null? attachment.getUrl(): command).toString(), TrackSchedulers.get(event.getGuildId()));
                    }
                } else {
                    sendMessage(event, "You need to be connected to a voice channel!");
                }
            } else {
                sendMessage(event, "An error occurred!");
            }
        } else {
            sendMessage(event, "You're not a member of this guild");
        }
    }

    }


