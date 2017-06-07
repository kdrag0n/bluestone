package com.khronodragon.bluestone.voice;

import net.dv8tion.jda.core.audio.AudioSendHandler;

public class DummySendHandler implements AudioSendHandler {
    @Override
    public boolean canProvide() {
        return false;
    }

    @Override
    public byte[] provide20MsAudio() {
        return new byte[] {};
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
