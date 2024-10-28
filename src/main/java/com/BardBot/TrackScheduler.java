package com.BardBot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.core.object.entity.channel.MessageChannel;

import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class TrackScheduler extends AudioEventAdapter implements AudioLoadResultHandler {

    public final AudioPlayer player;
    private List<String> command;
    private VoiceConnection voiceConnection;
    private int mode = 0;
    MessageChannel mainMessageChannel;

    final private AudioPlayerManager playerManager;
    public AudioProvider provider;
    public List<AudioTrack> queue = new ArrayList<>();

     TrackScheduler(AudioPlayer player, List<String> command, AudioProvider audioProvider, MessageChannel mainMessageChannel, AudioPlayerManager playerManager){
        this.player = player;
        this.command = command;
        this.provider = audioProvider;
        this.mainMessageChannel = mainMessageChannel;
        this.playerManager = playerManager;
    }

    public void setVoiceConnection(VoiceConnection voiceConnection){
         this.voiceConnection = voiceConnection;
    }

    public void setCommand(List<String> command){
         this.command = command;
    }

    //0 - off,  1 - single,  2 - all
    public void changeLoop(int mode){ this.mode = mode; }

    public void skip(){ player.stopTrack(); }

    public void clearQueue(){
         queue.clear();
         queue.add(player.getPlayingTrack().makeClone());
     }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
         if(queue.isEmpty()){
             queue.add(audioTrack);
             player.playTrack(audioTrack);
         }else{
             queue.add(audioTrack);
             mainMessageChannel.createMessage(Embeds.embed.withDescription("Added " + audioTrack.getInfo().title + " to queue")).subscribe();
         }
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
         if(queue.isEmpty()){
             queue.addAll(audioPlaylist.getTracks());
             player.playTrack(queue.get(0));
             mainMessageChannel.createMessage(Embeds.embed.withDescription("Added " + audioPlaylist.getName() + " to queue")).subscribe();
         }else{
             queue.addAll(audioPlaylist.getTracks());
             mainMessageChannel.createMessage(Embeds.embed.withDescription("Added " + audioPlaylist.getName() + " to queue")).subscribe();
         }
    }

    @Override
    public void noMatches() {
         if(command != null) {
             try {
                 List<String> results = Search(command, 1);
                 playerManager.loadItem(results.get(0), this);

             } catch (IOException e) {
                 mainMessageChannel.createMessage(Embeds.embed.withDescription("Something went wrong"));
             }
         }else{
             mainMessageChannel.createMessage(Embeds.embed.withDescription("No matches found"));
         }
    }

    @Override
    public void loadFailed(FriendlyException e1) {
        if(command != null) {
            try {
                List<String> results = Search(command, 1);
                playerManager.loadItem(results.get(0), this);

            } catch (IOException e) {
                mainMessageChannel.createMessage(Embeds.embed.withDescription("Something went wrong"));
            }
        }else{
            mainMessageChannel.createMessage(Embeds.embed.withDescription("No matches found"));
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
         if(mode == 1){super.onTrackStart(player,track);}
        mainMessageChannel.createMessage(Embeds.embed.withDescription("Now playing " + track.getInfo().title)).subscribe();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

         switch (mode){
             case 0:
                 mainMessageChannel.createMessage(Embeds.embed.withDescription("Just stopped playing " + track.getInfo().title)).subscribe();
                 queue.remove(0);
                 if(queue.isEmpty()){
                     voiceConnection.disconnect().subscribe();
                 }else {
                     player.playTrack(queue.get(0));
                 }
                 break;
             case 1:
                 player.playTrack(track.makeClone());
                break;
             case 2:
                 mainMessageChannel.createMessage(Embeds.embed.withDescription("Just stopped playing " + track.getInfo().title)).subscribe();
                 queue.add(track.makeClone());
                 queue.remove(0);
                 player.playTrack(queue.get(0));
                break;
         }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
         player.playTrack(track);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        super.onTrackStuck(player, track, thresholdMs);
    }

    private static ArrayList<String> Search(List<String> commands, int x) throws IOException {
        StringBuilder title = new StringBuilder();

        for(String command : commands){
            if(title.length() == 0){
                title = new StringBuilder(command);
            }else{
                title.append("+").append(command);
            }
        }

        String url = "https://www.youtube.com/results?search_query=" + title;
        final Document document = Jsoup.connect(url).get();

        String[] array;
        ArrayList<String> list= new ArrayList<>();
        LinkedHashSet<String> hash = new LinkedHashSet<>();

        int y = 0, d = 0;

        for (Element row : document.select("Script")) {
            if (hash.size() == x){
                break;
            }

            if (row.html() != null) {
                array = row.html().split("[^\\w-]+");

                for (String s : array){
                    if (hash.size() == x){
                        break;
                    }

                    if (!s.isBlank()) {

                        if(y == 5 && d == 1){
                            hash.add(s);
                            d=0;
                        }
                        if(s.equals("WEB_PAGE_TYPE_WATCH")){
                            y=0;
                            d=1;
                        }
                        y++;

                    }
                }
            }
        }

        String[] string = new String[hash.size()];
        string = hash.toArray(string);

        if(x > string.length){
            System.out.println("Displaying only: " + string.length + " Titles!");
        }
        for (int i = 0; i < x && i < string.length; i++) {
            list.add("https://www.youtube.com/watch?v=" + string[i]);
        }

        return list;

    }
}
