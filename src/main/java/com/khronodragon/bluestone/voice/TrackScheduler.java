package com.khronodragon.bluestone.voice;

import com.khronodragon.bluestone.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.*;

public class TrackScheduler extends AudioEventAdapter {
    private boolean repeating = false;
    public final AudioPlayer player;
    public final Queue<AudioTrack> queue = new LinkedList<>();
    public final Map<AudioTrack, MessageChannel> channelMap = new HashMap<>();
    AudioTrack lastTrack;
    AudioTrack current;

    TrackScheduler(AudioPlayer player) {
        this.player = player;
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true))
            queue.offer(track);
    }

    public void queue(AudioTrack track, MessageChannel textChannel) {
        queue(track);
        channelMap.put(track, textChannel);
    }

    public void nextTrack() {
        player.startTrack(queue.poll(), false);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        current = track;
        if (channelMap.containsKey(track)) {
            AudioTrackInfo info = track.getInfo();
            channelMap.get(track).sendMessage("Now playing **" + mentionClean(info.title) + "**, duration **" + Bot.formatDuration(track.getDuration()) + "**").queue();
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        lastTrack = track;
        if (endReason.mayStartNext) {
            if (repeating) {
                player.startTrack(track.makeClone(), false);
                if (channelMap.containsKey(track)) {
                    AudioTrackInfo info = track.getInfo();
                    channelMap.get(track).sendMessage("Now playing **" + mentionClean(info.title) + "**, duration **" + Bot.formatDuration(track.getDuration()) + "**").queue();
                }
            } else {
                nextTrack();
            }
        } else if (endReason == endReason.CLEANUP) {
            if (channelMap.containsKey(track)) {
                queue(track.makeClone(), channelMap.get(track));
            } else {
                queue(track.makeClone());
            }
        }
        if (channelMap.containsKey(track)) {
            channelMap.remove(track);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (channelMap.containsKey(track)) {
            channelMap.get(track).sendMessage(":bangbang: Error in audio player! " + exception.getMessage());
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (channelMap.containsKey(track)) {
            channelMap.get(track).sendMessage("Song appears to be frozen, skipping.").queue();
        }
        track.stop();
        nextTrack();
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public void shuffleQueue() {
        Collections.shuffle((List<?>) queue);
    }

    private String mentionClean(String in) {
        return in.replace("@everyone", "@\u200beveryone").replace("@here", "@\u200bhere");
    }
}
