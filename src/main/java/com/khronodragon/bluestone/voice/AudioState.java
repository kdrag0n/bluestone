package com.khronodragon.bluestone.voice;

import com.khronodragon.bluestone.cogs.MusicCog;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Date;

public class AudioState {
    public AudioPlayer player;
    public TrackScheduler scheduler;
    public Guild guild;
    MusicCog parent;
    public Date creationTime = new Date();

    public AudioState(AudioPlayerManager manager, Guild guild, MusicCog parent) {
        player = manager.createPlayer();
        this.guild = guild;
        this.parent = parent;
        scheduler = new TrackScheduler(player, this);
        player.addListener(scheduler);
    }
}
