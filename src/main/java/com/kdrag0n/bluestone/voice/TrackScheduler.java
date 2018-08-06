package com.kdrag0n.bluestone.voice;

import com.kdrag0n.bluestone.util.Strings;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.util.*;

public class TrackScheduler extends AudioEventAdapter {
    private static final DummySendHandler dummyHandler = new DummySendHandler();
    private boolean repeating = false;
    private boolean emptyPaused = false;
    private Date emptyPauseTime = new Date();
    public final AudioPlayer player;
    public final Queue<AudioTrack> queue = new LinkedList<>();
    public AudioTrack current;
    private AudioState state;

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

    private void queue(AudioTrack track) {
        if (!player.startTrack(track, true))
            queue.offer(track);
    }

    public void queue(AudioTrack track, ExtraTrackInfo info) {
        track.setUserData(info);
        queue(track);
    }

    private void nextTrack() {
        if (queue.size() > 0) {
            player.startTrack(queue.poll(), false);
        } else {
            player.destroy();

            com.kdrag0n.bluestone.Bot.threadExecutor.execute(state.guild.getAudioManager()::closeAudioConnection);
            state.guild.getAudioManager().setSendingHandler(dummyHandler);
            state.parent.audioStates.remove(state.guild.getIdLong());
        }
    }

    public void skip() {
        onTrackEnd(player, current, AudioTrackEndReason.FINISHED);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        try {
            current = track;
            if (track.getUserData() != null && track.getUserData(ExtraTrackInfo.class).sendNowPlaying) {
                AudioTrackInfo info = track.getInfo();
                track.getUserData(ExtraTrackInfo.class).textChannel
                        .sendMessage("➡ **" + com.kdrag0n.bluestone.Context.filterMessage(info.title) + "**, length **" +
                                Strings.formatDuration(info.length / 1000L) + "**").queue();
            }
        } catch (PermissionException ignored) {}
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        current = null;

        try {
            track.stop();
        } catch (Throwable ignored) {}

        if (endReason.mayStartNext) {
            if (repeating) {
                AudioTrack clone = track.makeClone();
                if (track.getUserData() != null) {
                    clone.setUserData(track.getUserData());
                    clone.getUserData(ExtraTrackInfo.class).sendNowPlaying = false;
                }
                player.startTrack(clone, false);
            } else {
                nextTrack();
            }
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (track.getUserData() != null) {
            try {
                track.getUserData(ExtraTrackInfo.class).textChannel
                        .sendMessage("‼ Error in audio player! " + exception.getMessage()).queue();
            } catch (PermissionException ignored) {}
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        if (track.getUserData() != null) {
            try {
                track.getUserData(ExtraTrackInfo.class).textChannel
                        .sendMessage(com.kdrag0n.bluestone.Emotes.getFailure() + " Song appears to be frozen, skipping.").queue();
            } catch (PermissionException ignored) {}
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
}
