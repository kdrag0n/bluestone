package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.voice.AudioState;
import com.khronodragon.bluestone.voice.DummySendHandler;
import com.khronodragon.bluestone.voice.PlayerSendHandler;
import com.khronodragon.bluestone.voice.TrackLoadHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MusicCog extends Cog {
    private final DefaultAudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public Map<Long, AudioState> audioStates = new HashMap<>();

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

    //@Command(name = "summon", desc = "Summon me to your voice channel.", guildOnly = true)
    public void summon(Context ctx) {
        VoiceChannel channel = ctx.member.getVoiceState().getChannel();
        if (channel == null) {
            ctx.send(":x: You aren't in a voice channel!").queue();
            return;
        }


        AudioManager manager = ctx.guild.getAudioManager();
        AudioState state = getAudioState(ctx.guild);
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
            summon(ctx);
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":octagonal_sign: You need to be in the same voice channel as me to do that!").queue();
            return;
        }

        AudioState state = getAudioState(ctx.guild);
        if (state.scheduler.queue.size() >= 10) {
            ctx.send(":x: There can only be up to 10 items in the queue!").queue();
            return;
        }
        final String term = String.join(" ", ctx.args);

        playerManager.loadItem(term, new TrackLoadHandler(ctx, state, playerManager, term));
    }

    @Command(name = "pause", desc = "Pause the player.", guildOnly = true)
    public void cmdPause(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":octagonal_sign: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild);

        if (state.player.isPaused()) {
            ctx.send(":shrug: Already paused.").queue();
        } else {
            state.player.setPaused(true);
            ctx.send(":pause_button: Paused.").queue();
        }
    }

    @Command(name = "resume", desc = "Resume the player.", guildOnly = true)
    public void cmdResume(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":octagonal_sign: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild);

        if (state.player.isPaused()) {
            state.player.setPaused(false);
            ctx.send(":arrow_forward: Resumed.").queue();
        } else {
            ctx.send(":shrug: Not paused.").queue();
        }
    }

    @Command(name = "shuffle", desc = "Shuffle the queue.", guildOnly = true)
    public void cmdShuffle(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":octagonal_sign: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild);

        state.scheduler.shuffleQueue();
        List<String> items = state.scheduler.queue.stream().map(t -> "**" + t.getInfo().title + "**").collect(Collectors.toList());
        ctx.send(":twisted_rightwards_arrows: Queue shuffled.\n    \u2022 " + String.join("\n    \u2022 ", items)).queue();
    }

    @Command(name = "repeat", desc = "Toggle repeating of the current song.", guildOnly = true)
    public void cmdRepeat(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":octagonal_sign: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild);

        if (state.scheduler.isRepeating()) {
            state.scheduler.setRepeating(false);
            ctx.send(":arrow_right: No longer repeating.").queue();
        } else {
            state.scheduler.setRepeating(true);
            ctx.send(":repeat: Now repeating song.").queue();
        }
    }

    private String renderInfo(AudioTrackInfo info) {
        StringBuilder builder = new StringBuilder();
        if (info.author != null) {
            builder.append("Uploader: *" + info.author + "*\n");
        }

        builder.append("Length: *" + Bot.formatDuration(info.length / 1000) + "*");
        return builder.toString();
    }

    @Command(name = "queue", desc = "Show the current queue.", guildOnly = true)
    public void cmdQueue(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild);

        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor("Voice Queue", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl());

        if (state.scheduler.current == null)
            builder.setDescription("Nothing is playing.");
        else {
            builder.setDescription("A song is playing.");
            AudioTrackInfo info = state.scheduler.current.getInfo();
            builder.addField("▶ " + info.title + " ◀", renderInfo(info), false);
        }

        switch (state.scheduler.queue.size()) {
            case 0:
                builder.appendDescription(" The queue is empty.");
                break;
            case 1:
                builder.appendDescription(" One song is queued.");
                break;
            default:
                builder.appendDescription(" There are " + state.scheduler.queue.size() + " songs queued.");
        }

        for (AudioTrack track: state.scheduler.queue) {
            AudioTrackInfo info = track.getInfo();
            builder.addField(info.title, renderInfo(info), true);
        }

        ctx.send(new MessageBuilder()
        .append(":notes::musical_note:")
        .setEmbed(builder.build())
        .build()).queue();
    }

    @Command(name = "skip", desc = "Skip the current song.", guildOnly = true)
    public void cmdSkip(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":octagonal_sign: You need to be in the same voice channel as me to do that!").queue();
            return;
        }
        AudioState state = getAudioState(ctx.guild);

        ctx.send("not done yet").queue();
    }

    @Command(name = "stop", desc = "Stop the player and disconnect.", aliases={"disconnect"}, guildOnly = true)
    public void cmdStop(Context ctx) {
        if (ctx.guild.getSelfMember().getVoiceState().getChannel() == null) {
            ctx.send(":x: I'm not in a voice channel...").queue();
            return;
        } else if (ctx.guild.getSelfMember().getVoiceState().getChannel().getIdLong() != ctx.member.getVoiceState().getChannel().getIdLong()) {
            ctx.send(":octagonal_sign: You need to be in the same voice channel as me to do that!").queue();
            return;
        }

        if (!audioStates.containsKey(ctx.guild.getIdLong())) {
            ctx.send(":bangbang: Failed to get the state for this guild. Something's terribly broken.").queue();
            return;
        }

        ctx.guild.getAudioManager().closeAudioConnection();
        ctx.guild.getAudioManager().setSendingHandler(new DummySendHandler());
        audioStates.remove(ctx.guild.getIdLong());

        if (ctx.invoker == "stop")
            ctx.send("Stopped.").queue();
        else
            ctx.send("Disconnected.").queue();
    }
}
