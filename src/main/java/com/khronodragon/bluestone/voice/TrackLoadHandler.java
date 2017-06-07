package com.khronodragon.bluestone.voice;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Context;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class TrackLoadHandler implements AudioLoadResultHandler {
    public static final String[] prefixes = {"", "ytsearch:", "scsearch:"};
    private final Context ctx;
    private final AudioState state;
    private int iteration;
    private final AudioPlayerManager manager;
    private final String term;

    public TrackLoadHandler(Context ctx, AudioState state, AudioPlayerManager man, String term) {
        this.ctx = ctx;
        this.state = state;
        this.term = term;
        manager = man;
        iteration = 0;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        state.scheduler.queue(track, ctx.channel);
        if (state.scheduler.queue.size() > 1) {
            AudioTrackInfo info = track.getInfo();
            ctx.send(":white_check_mark: Queued **" + info.title + "** by **" + info.author + "**, length **" + Bot.formatDuration(info.length / 1000L) + "**").queue();
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        long duration = 0L;

        for (AudioTrack track: playlist.getTracks()) {
            state.scheduler.queue(track, ctx.channel);
            duration += track.getDuration();
        }
        ctx.send(":white_check_mark: Queued playlist **" + playlist.getName() + "**, length **" + Bot.formatDuration(duration / 1000L) + "**").queue();
    }

    @Override
    public void noMatches() {
        if (iteration < prefixes.length - 1) {
            iteration += 1;
            manager.loadItem(prefixes[iteration] + term, this);
        } else
            ctx.send(":warning: No matches found!").queue();
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        if (exception.severity == FriendlyException.Severity.COMMON) {
            ctx.send(":exclamation: " + exception.getMessage()).queue();
        } else {
            exception.printStackTrace();
            if (iteration < prefixes.length - 1) {
                iteration += 1;
                manager.loadItem(prefixes[iteration] + term, this);
            } else
                ctx.send(":bangbang: Failed to load song, maybe try a different source?").queue();
        }
    }
}
