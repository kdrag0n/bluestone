package com.kdrag0n.bluestone.cogs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.dao.Dao;
import com.kdrag0n.bluestone.Context;
import com.kdrag0n.bluestone.util.NullValueWrapper;
import com.kdrag0n.bluestone.sql.GuildMusicSettings;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import gnu.trove.impl.sync.TSynchronizedLongObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.entities.impl.VoiceChannelImpl;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MusicCog extends com.kdrag0n.bluestone.Cog {
    private ScheduledThreadPoolExecutor bgExecutor = new ScheduledThreadPoolExecutor(2, new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Music Cog Cleanup Thread %d")
            .build());
    private final DefaultAudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Dao<GuildMusicSettings, Long> settingsDao;
    public final TLongObjectMap<com.kdrag0n.bluestone.voice.AudioState> audioStates = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());

    public MusicCog(com.kdrag0n.bluestone.Bot bot) {
        super(bot);

        playerManager.setItemLoaderThreadPoolSize(16);

        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        bgExecutor.scheduleWithFixedDelay(this::doCleanup, 5, 5, TimeUnit.MINUTES);

        settingsDao = setupDao(GuildMusicSettings.class);
    }

    public String getName() {
        return "Music";
    }
    public String getDescription() {
        return "Listen to those beats together!";
    }

    public void unload() {
        playerManager.shutdown();
        bgExecutor.shutdown();
        super.unload();
    }

    private com.kdrag0n.bluestone.voice.AudioState getAudioState(Guild guild) {
        long guildId = guild.getIdLong();

        if (audioStates.containsKey(guildId)) {
            return audioStates.get(guildId);
        } else {
            com.kdrag0n.bluestone.voice.AudioState state = new com.kdrag0n.bluestone.voice.AudioState(playerManager, guild, this);
            audioStates.put(guildId, state);
            return state;
        }
    }

    public int getTracksLoaded() {
        int num = 0;
        for (com.kdrag0n.bluestone.voice.AudioState state: audioStates.valueCollection()) {
            num += state.scheduler.queue.size();

            if (state.scheduler.current != null)
                num++;
        }

        return num;
    }

    public int getActiveStreamCount() {
        int num = 0;

        for (com.kdrag0n.bluestone.voice.AudioState state: audioStates.valueCollection()) {
            if (state.scheduler.current != null && !state.scheduler.player.isPaused())
                num++;
        }

        return num;
    }

    @com.kdrag0n.bluestone.annotations.EventHandler
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (getVoiceEventSelfId(event) == event.getChannelLeft().getIdLong()) {
            com.kdrag0n.bluestone.voice.AudioState state = getAudioState(event.getGuild());
            if (state == null) return;

            if (((VoiceChannelImpl) event.getChannelLeft())
                    .getConnectedMembersMap().valueCollection().stream()
                    .filter(m -> !m.getVoiceState().isDeafened()).count() < 2) {
                state.scheduler.player.setPaused(true);
                state.scheduler.setEmptyPauseTime(new Date());
                state.scheduler.setEmptyPaused(true);
            }
        }
    }

    @com.kdrag0n.bluestone.annotations.EventHandler
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (getVoiceEventSelfId(event) == event.getChannelJoined().getIdLong()) {
            com.kdrag0n.bluestone.voice.AudioState state = getAudioState(event.getGuild());
            if (state == null) return;

            if (((VoiceChannelImpl) event.getChannelJoined())
                    .getConnectedMembersMap().valueCollection().stream()
                    .filter(m -> !m.getVoiceState().isDeafened()).count() == 2) {
                state.scheduler.player.setPaused(false);
                state.scheduler.setEmptyPaused(false);
            }
        }
    }

    private long getVoiceEventSelfId(GenericGuildVoiceEvent event) {
        try {
            Member member = event.getGuild().getSelfMember();
            GuildVoiceState vs = member.getVoiceState();
            VoiceChannel ch = vs.getChannel();

            if (ch == null) {
                return 0L;
            } else {
                return ch.getIdLong();
            }
        } catch (NullPointerException e) {
            return 0L;
        }
    }

    private void doCleanup() {
        audioStates.forEachEntry((guildId, state) -> {
            if (state.scheduler.current == null && state.scheduler.queue.size() < 1) {
                state.guild.getAudioManager().closeAudioConnection();
                state.guild.getAudioManager().setSendingHandler(new com.kdrag0n.bluestone.voice.DummySendHandler());
                audioStates.remove(guildId);

                return true;
            }

            if (new Date().getTime() - state.creationTime.getTime() < TimeUnit.MINUTES.toMillis(3)) {
                return true;
            }

            if (state.scheduler.isEmptyPaused()) {
                if (new Date().getTime() - state.scheduler.getEmptyPauseTime().getTime() < TimeUnit.MINUTES.toMillis(10)) {
                    return true;
                }

                state.guild.getAudioManager().closeAudioConnection();
                state.guild.getAudioManager().setSendingHandler(new com.kdrag0n.bluestone.voice.DummySendHandler());
                audioStates.remove(guildId);
            }

            return true;
        });
    }

    private void channelChecks(com.kdrag0n.bluestone.Context ctx) {
        VoiceChannel ch = ctx.member.getVoiceState().getChannel();
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.fail("I'm not in a voice channel...");
            throw new com.kdrag0n.bluestone.errors.PassException();
        } else if (ch == null) {
            ctx.fail("You're not in a voice channel!");
            throw new com.kdrag0n.bluestone.errors.PassException();
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ch.getIdLong()) {
            ctx.send("🛑 You need to be in the same voice channel as me to do that!").queue();
            throw new com.kdrag0n.bluestone.errors.PassException();
        }
    }

    private void summon(com.kdrag0n.bluestone.Context ctx) {
        VoiceChannel channel = ctx.member.getVoiceState().getChannel();
        if (channel == null) {
            ctx.fail("You aren't in a voice channel!");
            throw new com.kdrag0n.bluestone.errors.PassException();
        }

        AudioManager manager = ctx.guild.getAudioManager();
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);
        manager.setSendingHandler(state.getSendHandler());
        try {
            manager.openAudioConnection(channel);
        } catch (PermissionException e) {
            ctx.fail("I don't have permission to join that channel!");
            throw new com.kdrag0n.bluestone.errors.PassException();
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "play", desc = "Play something!", usage = "[search terms / link]", guildOnly = true)
    public void cmdPlay(com.kdrag0n.bluestone.Context ctx) throws SQLException {
        if (ctx.args.empty) {
            ctx.fail("I need something to play!");
            return;
        }

        try {
            if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
                summon(ctx);
            } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
                ctx.send("🛑 You need to be in the same voice channel as me to do that!").queue();
                return;
            }
        } catch (NullPointerException ignored) {
            ctx.fail("We both need to be connected to a voice channel!");
            return;
        }

        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);
        boolean isPatron = com.kdrag0n.bluestone.Permissions.check(ctx, com.kdrag0n.bluestone.Permissions.PATREON_SUPPORTER);
        int mn = isPatron ? 48 : 12;

        if (state.scheduler.queue.size() >= mn) {
            ctx.fail("There can only be up to " + mn + " items in the queue for you!");
            return;
        }

        final String term = ctx.args.join(' ');
        ctx.message.addReaction("⌛").queue();

        GuildMusicSettings settings = settingsDao.queryForId(ctx.guild.getIdLong());
        playerManager.loadItem(term, new com.kdrag0n.bluestone.voice.TrackLoadHandler(ctx, isPatron, state, playerManager, term, settings));
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "pause", desc = "Pause the player.", guildOnly = true)
    public void cmdPause(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);

        if (state.player.isPaused()) {
            ctx.send("🤷 Already paused.").queue();
        } else {
            state.player.setPaused(true);
            ctx.send("⏸ Paused.").queue();
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "resume", desc = "Resume the player.", guildOnly = true)
    public void cmdResume(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);

        if (state.player.isPaused()) {
            state.player.setPaused(false);
            ctx.send("➡ Resumed.").queue();
        } else {
            ctx.send("🤷 Not paused.").queue();
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "shuffle", desc = "Shuffle the queue.", guildOnly = true)
    public void cmdShuffle(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);

        state.scheduler.shuffleQueue();
        List<String> items = state.scheduler.queue.stream().map(t -> "**" + t.getInfo().title + "**").collect(Collectors.toList());
        ctx.send("🔀 Queue shuffled.\n    \u2022 " + String.join("\n    \u2022 ", items)).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "repeat", desc = "Toggle repeating of the current track.", guildOnly = true, aliases = {"loop"})
    public void cmdRepeat(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);

        if (state.scheduler.isRepeating()) {
            state.scheduler.setRepeating(false);
            ctx.send("➡ No longer repeating.").queue();
        } else {
            state.scheduler.setRepeating(true);
            ctx.send("🔁 Now repeating.").queue();
        }
    }

    private String renderInfo(AudioTrackInfo info) {
        StringBuilder builder = new StringBuilder();
        if (info.author != null) {
            builder.append("Uploader: *").append(info.author).append("*\n");
        }

        builder.append("Length: *").append(com.kdrag0n.bluestone.util.Strings.formatDuration(info.length / 1000)).append('*');
        return builder.toString();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "queue", desc = "Show the current queue.", guildOnly = true)
    public void cmdQueue(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.fail("I'm not in a voice channel...");
            return;
        }
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);

        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor("Voice Queue", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setColor(NullValueWrapper.val(ctx.member.getColor()).or(com.kdrag0n.bluestone.Cog::randomColor))
                .setTimestamp(Instant.now());

        if (state.scheduler.current == null)
            builder.setDescription("Nothing is playing.");
        else {
            builder.setDescription("A track is playing.");
            AudioTrackInfo info = state.scheduler.current.getInfo();
            builder.addField("▶ " + info.title + " ◀", renderInfo(info), false);
        }

        switch (state.scheduler.queue.size()) {
            case 0:
                builder.appendDescription(" The queue is empty.");
                break;
            case 1:
                builder.appendDescription(" One track is queued.");
                break;
            default:
                builder.appendDescription(" There are " + state.scheduler.queue.size() + " tracks queued.");
                break;
        }

        for (AudioTrack track: state.scheduler.queue) {
            AudioTrackInfo info = track.getInfo();
            builder.addField(info.title, renderInfo(info), false);
        }

        try {
            ctx.send(builder.build()).queue();
        } catch (IllegalArgumentException e) {
            ctx.fail("Queue too long to be displayed!");
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "skip", desc = "Skip the current track.", guildOnly = true)
    public void cmdSkip(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);
        if (state.scheduler.current == null) {
            ctx.send("There's no current track.").queue();
            return;
        }

        com.kdrag0n.bluestone.voice.ExtraTrackInfo info = state.scheduler.current.getUserData(com.kdrag0n.bluestone.voice.ExtraTrackInfo.class);
        if (info == null) {
            ctx.fail("The current track is missing a state!");
            return;
        }

        if (ctx.member.getUser().getIdLong() == info.requester.getUser().getIdLong()) {
            if (state.scheduler.queue.isEmpty())
                ctx.send("Skipped.").queue();

            state.scheduler.skip();
        } else {
            int targetVotes = (int) Math.ceil(((VoiceChannelImpl)ctx.guild.getSelfMember().getVoiceState().getChannel())
                    .getConnectedMembersMap().size() / 2.0f);
            if (info.hasVotedToSkip(ctx.member)) {
                ctx.send("You've already voted to skip this track. Votes: **[" + info.getSkipVotes() + '/' + targetVotes + "]**").queue();
            } else {
                if (info.getSkipVotes() == targetVotes - 1) {
                    state.scheduler.skip();
                    ctx.send("Skip vote passed.").queue();
                } else {
                    info.addSkipVote(ctx.member);
                    ctx.send("Skip vote added. Votes: **[" + info.getSkipVotes() + '/' + targetVotes + "]**").queue();
                }
            }
        }
    }

    @com.kdrag0n.bluestone.Perm.Kick
    @com.kdrag0n.bluestone.Perm.Voice.Mute
    @com.kdrag0n.bluestone.Perm.Voice.Deafen
    @com.kdrag0n.bluestone.Perm.Voice.Move
    @com.kdrag0n.bluestone.annotations.Command(name = "force_skip", desc = "Force skip the current track.", aliases = {"forceskip"}, guildOnly = true)
    public void cmdForceSkip(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);
        if (state.scheduler.current == null) {
            ctx.send("There's no current track.").queue();
            return;
        }

        com.kdrag0n.bluestone.voice.ExtraTrackInfo info = state.scheduler.current.getUserData(com.kdrag0n.bluestone.voice.ExtraTrackInfo.class);
        if (info == null) {
            ctx.fail("The current track is missing a state!");
            return;
        }

        if (state.scheduler.queue.isEmpty())
            ctx.send("Skipped.").queue();

        state.scheduler.skip();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "playing", desc = "Get the current track.", aliases = {"current", "np"}, guildOnly = true)
    public void cmdCurrent(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);
        com.kdrag0n.bluestone.voice.AudioState state = getAudioState(ctx.guild);

        if (state.scheduler.current == null)
            ctx.send("There's no current track.").queue();
        else {
            AudioTrackInfo info = state.scheduler.current.getInfo();
            ctx.send("▶ **" + info.title + "**, length **" + com.kdrag0n.bluestone.util.Strings.formatDuration(info.length / 1000) + "**").queue();
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "stop", desc = "Stop the player and disconnect.", aliases = {"disconnect"}, guildOnly = true)
    public void cmdStop(com.kdrag0n.bluestone.Context ctx) {
        channelChecks(ctx);

        if (!audioStates.containsKey(ctx.guild.getIdLong())) {
            ctx.send("‼ Failed to get the state for this server. Something's terribly broken.").queue();
            return;
        }

        ctx.guild.getAudioManager().closeAudioConnection();
        ctx.guild.getAudioManager().setSendingHandler(new com.kdrag0n.bluestone.voice.DummySendHandler());
        audioStates.remove(ctx.guild.getIdLong());

        if (ctx.invoker.equals("stop"))
            ctx.send("Stopped.").queue();
        else
            ctx.send("Disconnected.").queue();
    }

    @com.kdrag0n.bluestone.Perm.ManageServer
    @com.kdrag0n.bluestone.Perm.Voice.Mute
    @com.kdrag0n.bluestone.Perm.Voice.Move
    @com.kdrag0n.bluestone.Perm.Voice.Deafen
    @com.kdrag0n.bluestone.Perm.ManagePermissions
    @com.kdrag0n.bluestone.annotations.Command(name = "play_first_result", desc = "Toggle the setting for always playing the first search result.",
            aliases = {"first_result", "always_play_first", "play_first", "playfirst", "apfr"}, guildOnly = true, thread = true)
    public void cmdPlayFirstResult(Context ctx) throws SQLException {
        GuildMusicSettings settings = settingsDao.queryForId(ctx.guild.getIdLong());
        if (settings == null)
            settings = new GuildMusicSettings(ctx.guild.getIdLong(), false);

        if (settings.alwaysPlayFirstResult()) {
            settings.setAlwaysPlayFirstResult(false);
            ctx.success("I will now give a selection of search results before playing.");
        } else {
            settings.setAlwaysPlayFirstResult(true);
            ctx.success("I will now automatically play the first search result.");
        }

        settingsDao.createOrUpdate(settings);
    }
}
