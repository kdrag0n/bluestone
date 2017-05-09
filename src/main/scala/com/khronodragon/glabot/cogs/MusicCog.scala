package com.khronodragon.glabot.cogs

import com.khronodragon.glabot.{Bot, Context}
import com.khronodragon.glabot.annotations.Command
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers

class MusicCog(botObj: Bot) extends Cog {
    override final val bot = botObj
    final val playerManager = new DefaultAudioPlayerManager()
    AudioSourceManagers.registerRemoteSources(playerManager)

    def getName(): String = {
        "Music"
    }

    def getDescription(): String = {
        "Who doesn't want some music?"
    }
}