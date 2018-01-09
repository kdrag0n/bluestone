package com.khronodragon.bluestone.cogs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.sql.GuildMusicSettings;
import com.khronodragon.bluestone.voice.*;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MusicCog extends Cog {
    private static final Logger logger = LogManager.getLogger(MusicCog.class);
    private ScheduledThreadPoolExecutor bgExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Music Cog Cleanup Thread %d")
            .build());
    private final DefaultAudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private Dao<GuildMusicSettings, Long> settingsDao;

    @VisibleForTesting
    public TLongObjectMap<AudioState> audioStates = new TLongObjectHashMap<>();

    public MusicCog(Bot bot) {
        super(bot);

        playerManager.setItemLoaderThreadPoolSize(16);

        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        bgExecutor.scheduleWithFixedDelay(this::doCleanup, 5, 5, TimeUnit.MINUTES);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), GuildMusicSettings.class);
        } catch (SQLException e) {
            logger.error("Failed to create music settings table!", e);
        }

        try {
            settingsDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), GuildMusicSettings.class);
        } catch (SQLException e) {
            logger.error("Failed to create music settings DAO!", e);
        }
    }

    public String getName() {
        return "Music";
    }
    public String getDescription() {
        return "Listen to some sick beats with your friends!";
    }

    public void unload() {
        playerManager.shutdown();
        bgExecutor.shutdown();
        super.unload();
    }

    private AudioState getAudioState(Guild guild) {
        long guildId = guild.getIdLong();

        if (audioStates.containsKey(guildId)) {
            return audioStates.get(guildId);
        } else {
            AudioState state = new AudioState(playerManager, guild, this);
            audioStates.put(guildId, state);
            return state;
        }
    }

    public int getTracksLoaded() {
        int num = 0;
        for (AudioState state: audioStates.valueCollection()) {
            num += state.scheduler.queue.size();

            if (state.scheduler.current != null)
                num++;
        }

        return num;
    }

    public int getActiveStreamCount() {
        int num = 0;

        for (AudioState state: audioStates.valueCollection()) {
            if (state.scheduler.current != null && !state.scheduler.player.isPaused())
                num++;
        }

        return num;
    }

    @EventHandler
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (getVoiceEventSelfId(event) == event.getChannelLeft().getIdLong()) {
            AudioState state = getAudioState(event.getGuild());
            if (state == null) return;

            if (((VoiceChannelImpl) event.getChannelLeft())
                    .getConnectedMembersMap().valueCollection().stream()
                    .filter(m -> !m.getVoiceState().isDeafened()).count() < 2) {
                state.scheduler.player.setPaused(true);
                state.scheduler.setEmptyPauseTime(new Date());
                state.scheduler.setEmptyPaused(true);

                ExtraTrackInfo info;
                if (state.scheduler.current == null) {
                    info = null;
                } else {
                    info = state.scheduler.current.getUserData(ExtraTrackInfo.class);
                }

                /*if (info != null) {
                    info.textChannel.sendMessage("Voice channel empty - player paused.").queue();
                }*/
            }
        }
    }

    @EventHandler
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (getVoiceEventSelfId(event) == event.getChannelJoined().getIdLong()) {
            AudioState state = getAudioState(event.getGuild());
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
                state.guild.getAudioManager().setSendingHandler(new DummySendHandler());
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
                state.guild.getAudioManager().setSendingHandler(new DummySendHandler());
                audioStates.remove(guildId);
            }

            return true;
        });
    }

    private void channelChecks(Context ctx) {
        VoiceChannel ch = ctx.member.getVoiceState().getChannel();
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.fail("I'm not in a voice channel...");
            throw new PassException();
        } else if (ch == null) {
            ctx.fail("You're not in a voice channel!");
            throw new PassException();
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ch.getIdLong()) {
            ctx.send("🛑 You need to be in the same voice channel as me to do that!").queue();
            throw new PassException();
        }
    }

    //@Command(name = "summon", desc = "Summon me to your voice channel.", guildOnly = true)
    public void summon(Context ctx) {
        VoiceChannel channel = ctx.member.getVoiceState().getChannel();
        if (channel == null) {
            ctx.fail("You aren't in a voice channel!");
            throw new PassException();
        }

        AudioManager manager = ctx.guild.getAudioManager();
        AudioState state = getAudioState(ctx.guild);
        manager.setSendingHandler(state.getSendHandler());
        try {
            manager.openAudioConnection(channel);
        } catch (PermissionException e) {
            ctx.fail("I don't have permission to join that channel!");
            throw new PassException();
        }
    }

    @Command(name = "play", desc = "Play something!", usage = "[search terms / link]", guildOnly = true)
    public void cmdPlay(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 1) {
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

        AudioState state = getAudioState(ctx.guild);
        boolean isPatron = Permissions.check(ctx, Permissions.PATREON_SUPPORTER);
        int mn = isPatron ? 48 : 12;

        if (state.scheduler.queue.size() >= mn) {
            ctx.fail("There can only be up to " + mn + " items in the queue for you!");
            return;
        }

        final String term = String.join(" ", ctx.args);
        ctx.message.addReaction("⌛").queue();

        GuildMusicSettings settings = settingsDao.queryForId(ctx.guild.getIdLong());
        playerManager.loadItem(term, new TrackLoadHandler(ctx, isPatron, state, playerManager, term, settings));
    }

    @Command(name = "pause", desc = "Pause the player.", guildOnly = true)
    public void cmdPause(Context ctx) {
        channelChecks(ctx);
        AudioState state = getAudioState(ctx.guild);

        if (state.player.isPaused()) {
            ctx.send("🤷 Already paused.").queue();
        } else {
            state.player.setPaused(true);
            ctx.send("⏸ Paused.").queue();
        }
    }

    @Command(name = "resume", desc = "Resume the player.", guildOnly = true)
    public void cmdResume(Context ctx) {
        channelChecks(ctx);
        AudioState state = getAudioState(ctx.guild);

        if (state.player.isPaused()) {
            state.player.setPaused(false);
            ctx.send("➡ Resumed.").queue();
        } else {
            ctx.send("🤷 Not paused.").queue();
        }
    }

    @Command(name = "shuffle", desc = "Shuffle the queue.", guildOnly = true)
    public void cmdShuffle(Context ctx) {
        channelChecks(ctx);
        AudioState state = getAudioState(ctx.guild);

        state.scheduler.shuffleQueue();
        List<String> items = state.scheduler.queue.stream().map(t -> "**" + t.getInfo().title + "**").collect(Collectors.toList());
        ctx.send("🔀 Queue shuffled.\n    \u2022 " + String.join("\n    \u2022 ", items)).queue();
    }

    @Command(name = "repeat", desc = "Toggle repeating of the current track.", guildOnly = true, aliases = {"loop"})
    public void cmdRepeat(Context ctx) {
        channelChecks(ctx);
        AudioState state = getAudioState(ctx.guild);

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
            builder.append("Uploader: *" + info.author + "*\n");
        }

        builder.append("Length: *" + Bot.formatDuration(info.length / 1000) + '*');
        return builder.toString();
    }

    @Command(name = "queue", desc = "Show the current queue.", guildOnly = true)
    public void cmdQueue(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.fail("I'm not in a voice channel...");
            return;
        }
        AudioState state = getAudioState(ctx.guild);

        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor("Voice Queue", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
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
            ctx.send(new MessageBuilder()
                    .append("🎶🎵")
                    .setEmbed(builder.build())
                    .build()).queue();
        } catch (IllegalArgumentException e) {
            ctx.fail("Queue too long to be displayed!");
        }
    }

    @Command(name = "skip", desc = "Skip the current track.", guildOnly = true)
    public void cmdSkip(Context ctx) {
        channelChecks(ctx);
        AudioState state = getAudioState(ctx.guild);
        if (state.scheduler.current == null) {
            ctx.send("There's no current track.").queue();
            return;
        }

        ExtraTrackInfo info = state.scheduler.current.getUserData(ExtraTrackInfo.class);
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

    @Perm.Kick
    @Perm.Voice.Mute
    @Perm.Voice.Deafen
    @Perm.Voice.Move
    @Command(name = "force_skip", desc = "Force skip the current track.", aliases = {"forceskip"}, guildOnly = true)
    public void cmdForceSkip(Context ctx) {
        channelChecks(ctx);
        AudioState state = getAudioState(ctx.guild);
        if (state.scheduler.current == null) {
            ctx.send("There's no current track.").queue();
            return;
        }

        ExtraTrackInfo info = state.scheduler.current.getUserData(ExtraTrackInfo.class);
        if (info == null) {
            ctx.fail("The current track is missing a state!");
            return;
        }

        if (state.scheduler.queue.isEmpty())
            ctx.send("Skipped.").queue();

        state.scheduler.skip();
    }

    @Command(name = "playing", desc = "Get the current track.", aliases = {"current", "np"}, guildOnly = true)
    public void cmdCurrent(Context ctx) {
        channelChecks(ctx);
        AudioState state = getAudioState(ctx.guild);

        if (state.scheduler.current == null)
            ctx.send("There's no current track.").queue();
        else {
            AudioTrackInfo info = state.scheduler.current.getInfo();
            ctx.send("▶ **" + info.title + "**, length **" + Bot.formatDuration(info.length / 1000) + "**").queue();
        }
    }

    @Command(name = "stop", desc = "Stop the player and disconnect.", aliases = {"disconnect"}, guildOnly = true)
    public void cmdStop(Context ctx) {
        channelChecks(ctx);

        if (!audioStates.containsKey(ctx.guild.getIdLong())) {
            ctx.send("‼ Failed to get the state for this server. Something's terribly broken.").queue();
            return;
        }

        ctx.guild.getAudioManager().closeAudioConnection();
        ctx.guild.getAudioManager().setSendingHandler(new DummySendHandler());
        audioStates.remove(ctx.guild.getIdLong());

        if (ctx.invoker.equals("stop"))
            ctx.send("Stopped.").queue();
        else
            ctx.send("Disconnected.").queue();
    }

    @Perm.ManageServer
    @Perm.Voice.Mute
    @Perm.Voice.Move
    @Perm.Voice.Deafen
    @Perm.ManagePermissions
    @Command(name = "play_first_result", desc = "Toggle the setting for always playing the first search result.",
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
