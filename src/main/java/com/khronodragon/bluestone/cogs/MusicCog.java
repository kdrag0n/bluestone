package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.voice.AudioState;
import com.khronodragon.bluestone.voice.PlayerSendHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class MusicCog extends Cog {
    private final DefaultAudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private Map<Long, AudioState> voiceStates = new HashMap<>();

    public MusicCog(Bot bot) {
        super(bot);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
    }

    public String getName() {
        return "Music";
    }
    public String getDescription() {
        return "Listen to some sick beats with your friends!";
    }

    private AudioState getAudioState(long guildId) {
        if (voiceStates.containsKey(guildId)) {
            return voiceStates.get(guildId);
        } else {
            AudioState state = new AudioState(playerManager);
            voiceStates.put(guildId, state);
            return state;
        }
    }

    @Command(name = "summon", desc = "Summon me to your voice channel.", guildOnly = true)
    public void cmdSummon(Context ctx) {
        VoiceChannel channel = ctx.member.getVoiceState().getChannel();
        if (channel == null) {
            ctx.send(":x: You aren't in a voice channel!").queue();
            return;
        }


        AudioManager manager = ctx.guild.getAudioManager();
        AudioState state = getAudioState(ctx.guild.getIdLong());
        manager.setSendingHandler(new PlayerSendHandler(state.player));
        try {
            manager.openAudioConnection(channel);
        } catch (PermissionException e) {
            ctx.send(":warning: I don't have permission to join that channel!").queue();
            return;
        }
    }

    @Command(name = "play", desc = "Play something!", usage = "[song]", guildOnly = true)
    public void cmdPlay(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            cmdSummon(ctx);
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":warning: You need to be in the same voice channel as me to do that!").queue();
            return;
        }

        AudioState state = getAudioState(ctx.guild.getIdLong());
        ctx.channel.sendTyping().queue();

        playerManager.loadItem(String.join(" ", ctx.args), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                state.scheduler.queue(track);
                if (state.scheduler.queue.size() > 1) {
                    AudioTrackInfo info = track.getInfo();
                    ctx.send("Queued **" + info.title + "** by **" + info.author + "**, duration **" + bot.formatDuration(track.getDuration()) + "**").queue();
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                long duration = 0l;

                for (AudioTrack track: playlist.getTracks()) {
                    state.scheduler.queue(track);
                    duration += track.getDuration();
                }
                ctx.send("Queued playlist **" + playlist.getName() + "**, duration **" + bot.formatDuration(duration) + "**").queue();
            }

            @Override
            public void noMatches() {
                ctx.send(":warning: No matches found!").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if (exception.severity == FriendlyException.Severity.COMMON) {
                    ctx.send(":exclamation: " + exception.getMessage()).queue();
                } else {
                    ctx.send(":bangbang: Failed to load song, try a different source?").queue();
                }
            }
        });
    }

    @Command(name = "pause", desc = "Pause the player.", aliases = {"resume"}, guildOnly = true)
    public void cmdPause(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":warning: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild.getIdLong());

        if (state.player.isPaused()) {
            state.player.setPaused(false);
            ctx.send(":arrow_forward: Resumed.").queue();
        } else {
            state.player.setPaused(true);
            ctx.send(":pause_button: Paused.").queue();
        }
    }

    @Command(name = "shuffle", desc = "Shuffle the queue.", guildOnly = true)
    public void cmdShuffle(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":warning: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild.getIdLong());

        state.scheduler.shuffleQueue();
        ctx.send(":twisted_rightwards_arrows: Shuffled.").queue();
    }

    @Command(name = "repeat", desc = "Toggle repeating of the current song.", guildOnly = true)
    public void cmdRepeat(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":warning: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild.getIdLong());

        if (state.scheduler.isRepeating()) {
            state.scheduler.setRepeating(false);
            ctx.send(":arrow_right: No longer repeating.").queue();
        } else {
            state.scheduler.setRepeating(true);
            ctx.send(":repeat: Now repeating song.").queue();
        }
    }
}
