package com.khronodragon.bluestone.voice;

import com.jagrosh.jdautilities.menu.orderedmenu.OrderedMenuBuilder;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.sql.GuildMusicSettings;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;

public class TrackLoadHandler implements AudioLoadResultHandler {
    private static final String[] PREFIXES = {"", "ytsearch:", "scsearch:"};
    private final Context ctx;
    private final AudioState state;
    private int iteration;
    private final AudioPlayerManager manager;
    private final String term;
    private final GuildMusicSettings settings;

    public TrackLoadHandler(Context ctx, AudioState state, AudioPlayerManager man, String term, GuildMusicSettings settings) {
        this.ctx = ctx;
        this.state = state;
        this.term = term;
        this.settings = settings;
        manager = man;
        iteration = 0;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        if (!track.getInfo().isStream && track.getDuration() > TimeUnit.MINUTES.toMillis(2 * 60 + 32)) {
            ctx.send("⛔ Track longer than **2 h 30 min**!").queue();
            Cog.removeReactionIfExists(ctx.message, "⌛");
            ctx.message.addReaction("❌").queue();
            return;
        }

        state.scheduler.queue(track, new ExtraTrackInfo(ctx.channel, ctx.member));
        if (!state.scheduler.queue.isEmpty()) {
            AudioTrackInfo info = track.getInfo();

            ctx.send(Emotes.getSuccess() + " Queued **" + info.title + "** by **" + info.author +
                    "**, length **" + Bot.formatDuration(info.length / 1000L) + "**").queue();
            Cog.removeReactionIfExists(ctx.message, "⌛");
            ctx.message.addReaction("✅").queue();
        }
    }

    private String formatTime(long duration) {
        if (duration == Long.MAX_VALUE)
            return "LIVE";

        long seconds = Math.round(duration / 1000.0);
        long hours = seconds / (60 * 60);
        seconds %= 60 * 60;
        long minutes = seconds / 60;
        seconds %= 60;

        return (hours > 0 ? hours + ":" : "") + (minutes < 10 ? "0" + minutes : minutes) +
                ":" + (seconds < 10 ? "0" + seconds : seconds);
    }

    private void searchResults(List<AudioTrack> tracks) {
        ctx.send("⌛ Pick a search result.").queue(msg -> {
            OrderedMenuBuilder builder = new OrderedMenuBuilder()
                    .allowTextInput(true)
                    .useCancelButton(true)
                    .useNumbers()
                    .setAction(i -> {
                        AudioTrack track;
                        try {
                            track = tracks.get(i - 1);
                        } catch (IndexOutOfBoundsException e) {
                            ctx.send(Emotes.getFailure() + " No such track!").queue();
                            return;
                        }

                        trackLoaded(track);
                    })
                    .setText("⌛ Pick a search result.")
                    .setCancel(() -> msg.delete().queue())
                    .setUsers(ctx.author) // from MessageReceivedEvent#getAuthor
                    .setEventWaiter(ctx.bot.getEventWaiter())
                    .setTimeout(20, TimeUnit.SECONDS);

            for (int i = 0; i < 5 && i < tracks.size(); i++) {
                AudioTrack track = tracks.get(i);
                AudioTrackInfo info = track.getInfo();

                builder.addChoices(format("`[{0}]` [**{1}** (by {2})]({3})",
                        formatTime(track.getDuration()), info.title, info.author,
                        info.uri));
            }

            builder.build().display(msg);
        });
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        if (playlist.isSearchResult()) {
            if (tracks.size() < 1)
                noMatches();
            else if (settings != null && settings.alwaysPlayFirstResult())
                trackLoaded(tracks.get(0));
            else
                searchResults(tracks);

            return;
        }
        if (tracks.size() > 24) {
            ctx.send(Emotes.getFailure() + " Playlist is longer than 24 tracks!").queue();
            return;
        }
        long duration = 0L;

        for (AudioTrack track: tracks) {
            if (!track.getInfo().isStream && track.getDuration() > TimeUnit.HOURS.toMillis(3)) {
                ctx.send("⛔ Track **" + track.getInfo().title + "** longer than **3 hours**!").queue();
                return;
            }
            state.scheduler.queue(track, new ExtraTrackInfo(ctx.channel, ctx.member));
            duration += track.getDuration();
        }
        ctx.send(Emotes.getSuccess() + " Queued playlist **" + playlist.getName() + "**, length **" + Bot.formatDuration(duration / 1000L) + "**").queue();
        Cog.removeReactionIfExists(ctx.message, "⌛");
        ctx.message.addReaction("✅").queue();
    }

    @Override
    public void noMatches() {
        if (iteration < PREFIXES.length - 1) {
            iteration += 1;
            manager.loadItem(PREFIXES[iteration] + term, this);
        } else {
            ctx.send(Emotes.getFailure() + " No matches found!").queue();
            Cog.removeReactionIfExists(ctx.message, "⌛");
            ctx.message.addReaction("❌").queue();
        }
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        ctx.send("‼ Error loading track: " + exception.getMessage()).queue();
        Cog.removeReactionIfExists(ctx.message, "⌛");
        ctx.message.addReaction("❌").queue();
    }
}
