package com.khronodragon.bluestone.voice;

import com.khronodragon.bluestone.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.*;

public class TrackScheduler extends AudioEventAdapter {
    private boolean repeating = false;
    private boolean emptyPaused = false;
    private Date emptyPauseTime = new Date();
    public final AudioPlayer player;
    public final Queue<AudioTrack> queue = new LinkedList<>();
    public final Map<AudioTrack, ExtraTrackInfo> infoMap = new HashMap<>();
    private AudioTrack lastTrack;
    public AudioTrack current;
    public AudioState state;

    public boolean isEmptyPaused() {
        return emptyPaused;
    }

    public void setEmptyPaused(boolean emptyPaused) {
        this.emptyPaused = emptyPaused;
    }

    public Date getEmptyPauseTime() {
        return emptyPauseTime;
    }

    public void setEmptyPauseTime(Date emptyPauseTime) {
        this.emptyPauseTime = emptyPauseTime;
    }

    TrackScheduler(AudioPlayer player, AudioState state) {
        this.player = player;
        this.state = state;
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true))
            queue.offer(track);
    }

    public void queue(AudioTrack track, ExtraTrackInfo info) {
        infoMap.put(track, info);
        queue(track);
    }

    public void nextTrack() {
        if (queue.size() > 0) {
            player.startTrack(queue.poll(), false);
        } else {
            player.destroy();
            state.guild.getAudioManager().closeAudioConnection();
            state.guild.getAudioManager().setSendingHandler(new DummySendHandler());
            state.parent.audioStates.remove(state.guild.getIdLong());
        }
    }

    public void skip() {
        onTrackEnd(player, current, AudioTrackEndReason.FINISHED);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        current = track;
        if (infoMap.containsKey(track)) {
            AudioTrackInfo info = track.getInfo();
            infoMap.get(track).textChannel.sendMessage(":arrow_forward: **" + mentionClean(info.title) + "**, length **" + Bot.formatDuration(info.length / 1000L) + "**").queue();
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        lastTrack = track;
        current = null;
        try {
            try {
                track.stop();
            } catch (Throwable e) {}

            if (endReason.mayStartNext) {
                if (repeating) {
                    AudioTrack clone = track.makeClone();
                    if (infoMap.containsKey(track)) {
                        infoMap.put(clone, infoMap.get(track));
                    }
                    player.startTrack(clone, false);
                } else {
                    nextTrack();
                }
            }
        } finally {
            if (infoMap.containsKey(track)) {
                infoMap.remove(track);
            }
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (infoMap.containsKey(track)) {
            infoMap.get(track).textChannel.sendMessage(":bangbang: Error in audio player! " + exception.getMessage());
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (infoMap.containsKey(track)) {
            infoMap.get(track).textChannel.sendMessage(":warning: Song appears to be frozen, skipping.").queue();
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
