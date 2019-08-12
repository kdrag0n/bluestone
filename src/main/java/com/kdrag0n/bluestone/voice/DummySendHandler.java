package com.kdrag0n.bluestone.voice;

import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class DummySendHandler implements AudioSendHandler {
    @Override
    public boolean canProvide() {
        return false;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return null;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
