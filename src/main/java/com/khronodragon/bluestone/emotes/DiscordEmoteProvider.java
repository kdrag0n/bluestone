package com.khronodragon.bluestone.emotes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordEmoteProvider implements EmoteProvider {
    private static final Pattern CUSTOM_EMOTE_PATTERN = Pattern.compile("^<:([a-z_]+):([0-9]{17,19})>$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean hasEmote(String emote) {
        return emote.matches(CUSTOM_EMOTE_PATTERN.pattern());
    }

    @Override
    public String getUrl(String emote) {
        return "https://cdn.discordapp.com/emojis/" + CUSTOM_EMOTE_PATTERN.matcher(emote).group(2) + ".png";
    }

    @Override
    public EmoteInfo getEmoteInfo(String emote) {
        Matcher matcher = CUSTOM_EMOTE_PATTERN.matcher(emote);
        return new EmoteInfo(matcher.group(1), matcher.group(2), null);
    }

    @Override
    public boolean isLoaded() {
        return true;
    }
}
