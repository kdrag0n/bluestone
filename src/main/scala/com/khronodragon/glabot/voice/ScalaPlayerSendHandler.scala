package com.khronodragon.glabot.voice

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler

class ScalaPlayerSendHandler(val player: AudioPlayer) extends AudioSendHandler {
    private final val audioPlayer: AudioPlayer = player
    private var lastFrame

    override def canProvide(): Boolean = {
        lastFrame = audioPlayer.provide()
        return lastFrame != null
    }

    override def provide20MsAudio(): Array[Byte] = {
        return lastFrame.data
    }

    override def isOpus(): Boolean = {
        return true
    }
}