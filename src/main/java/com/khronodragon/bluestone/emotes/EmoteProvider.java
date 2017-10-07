package com.khronodragon.bluestone.emotes;

public interface EmoteProvider {
    boolean hasEmote(String emote);
    String getUrl(String emote);
    EmoteInfo getEmoteInfo(String emote);
}
