package com.khronodragon.bluestone.cogs

import com.khronodragon.bluestone.{Bot, Context}
import com.khronodragon.bluestone.annotations.Command
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers

class MusicCog(botObj: Bot) extends Cog {
    override final val bot = botObj
    final val playerManager = new DefaultAudioPlayerManager()
    AudioSourceManagers.registerRemoteSources(playerManager)

    def getName(): String = "Music"

    def getDescription(): String = "Who doesn't want some music?"
}
