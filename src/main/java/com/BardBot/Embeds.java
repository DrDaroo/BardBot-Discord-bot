package com.BardBot;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class Embeds {

    public static EmbedCreateSpec statusEmbed(AudioTrack track){
            long timeLeft = (track.getInfo().length - track.getPosition()) / 1000;
            final long secondsLeft = timeLeft % 60;
            final long minutesLeft = (timeLeft - secondsLeft) / 60;

        return EmbedCreateSpec.builder()
                .title(track.getInfo().title)
                .url(track.getInfo().uri)
                .image("https://img.youtube.com/vi/" + track.getIdentifier() + "/hqdefault.jpg")
                .addField("Time to end: ",minutesLeft + ":" + secondsLeft  ,(true))
                .addField("Author: ", track.getInfo().author, true)
                .build();
    }

    static public EmbedCreateSpec helpEmbed(String prefix){
        return EmbedCreateSpec.builder()
                .color(Color.RED)
                .title("Command list")
                .addField(prefix + "play *'url/song name'*","play currently typed track",false )
                .addField(prefix + "skip","skips currently played song",false)
                .addField(prefix + "loop", "*loop all* loops current queue, *loop single* loops current track, *loop off* turns off loop mode", false)
                .addField(prefix + "queue","shows current existing queue",false)
                .addField(prefix + "clear","clears current existing queue",false)
                .footer("Daroo", null)
                .url("https://github.com/DrDaroo/BardBot-Discord-bot")
                .build();

    }


    final static public EmbedCreateSpec embed = EmbedCreateSpec.builder()
            .color(Color.RED)
            .description("You need to be connected to a voice channel!")
            .build();

}
