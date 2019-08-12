package com.kdrag0n.bluestone.voice;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class PlayerSendHandler implements AudioSendHandler {
    private static final Logger logger = LoggerFactory.getLogger(PlayerSendHandler.class);
    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;
    private ByteBuffer dataBuffer;

    public PlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        int len = lastFrame.getDataLength();
        if (dataBuffer == null || dataBuffer.capacity() < len) {
            dataBuffer = ByteBuffer.allocate(len);
        } else {
            dataBuffer.rewind();
            dataBuffer.limit(len);
        }

        lastFrame.getData(dataBuffer.array(), 0);
        return dataBuffer;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
