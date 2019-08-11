package com.kdrag0n.bluestone.voice;

import com.kdrag0n.bluestone.modules.MusicModule;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Date;

public class AudioState {
    public AudioPlayer player;
    public TrackScheduler scheduler;
    public Guild guild;
    MusicModule parent;
    public Date creationTime = new Date();
    private PlayerSendHandler sendHandler;

    public AudioState(AudioPlayerManager manager, Guild guild, MusicModule parent) {
        player = manager.createPlayer();
        this.guild = guild;
        this.parent = parent;
        scheduler = new TrackScheduler(player, this);
        player.addListener(scheduler);
        sendHandler = new PlayerSendHandler(player);
    }

    public PlayerSendHandler getSendHandler() {
        return sendHandler;
    }
}
