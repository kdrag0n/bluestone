package com.kdrag0n.bluestone.emotes;

interface EmoteProvider {
    boolean hasEmote(String emote);
    String getUrl(String emote);
    EmoteInfo getEmoteInfo(String emote);
}
