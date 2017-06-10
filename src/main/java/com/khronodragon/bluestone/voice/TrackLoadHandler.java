package com.khronodragon.bluestone.voice;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Context;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackLoadHandler implements AudioLoadResultHandler {
    private static final String[] prefixes = {"", "ytsearch:", "scsearch:"};
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
        if (!track.getInfo().isStream && track.getDuration() > TimeUnit.MINUTES.toMillis(2 * 60 + 32)) {
            ctx.send(":no_entry: Track longer than **2 h 30 min**!").queue();
            return;
        }

        state.scheduler.queue(track, new ExtraTrackInfo(ctx.channel, ctx.member));
        if (!state.scheduler.queue.isEmpty()) {
            AudioTrackInfo info = track.getInfo();

            ctx.send(":white_check_mark: Queued **" + info.title + "** by **" + info.author + "**, length **" + Bot.formatDuration(info.length / 1000L) + "**").queue();
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        if (playlist.isSearchResult()) {
            if (tracks.size() == 0)
                noMatches();
            else
                trackLoaded(tracks.get(0));

            return;
        }
        long duration = 0L;

        for (AudioTrack track: tracks) {
            if (!track.getInfo().isStream && track.getDuration() > TimeUnit.MINUTES.toMillis(2 * 60 + 32)) {
                ctx.send(":no_entry: Track **" + track.getInfo().title + "** longer than **2 h 30 min**!").queue();
                return;
            }
            state.scheduler.queue(track, new ExtraTrackInfo(ctx.channel, ctx.member));
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
        ctx.send(":bangbang: Error loading track: " + exception.getMessage()).queue();
    }
}
