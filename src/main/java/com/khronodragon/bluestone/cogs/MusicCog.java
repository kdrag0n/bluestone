package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

public class MusicCog extends Cog {
    private final DefaultAudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public MusicCog(Bot bot) {
        super(bot);
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    public String getName() {
        return "Music";
    }
    public String getDescription() {
        return "Listen to some sick beats with your friends!";
    }
}
