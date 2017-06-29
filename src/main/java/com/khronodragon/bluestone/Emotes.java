package com.khronodragon.bluestone;

import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;
import org.apache.commons.lang3.text.WordUtils;

public class Emotes {
    private static boolean hasDbots = false;
    private static boolean hasJda = false;

    static void setHasDbots(boolean hasDbots) {
        Emotes.hasDbots = hasDbots;
    }

   static void setHasJda(boolean hasJda) {
        Emotes.hasJda = hasJda;
    }

    public static boolean hasDbots() {
        return hasDbots;
    }

    public static boolean hasJda() {
        return hasJda;
    }

    public static String getFullMemberStatus(Member member) {
        if (hasDbots)
            return getMemberStatus(member) + ' ' + WordUtils.capitalizeFully(member.getOnlineStatus()
                    .name().replace('_', ' '));
        else
            return getMemberStatus(member);
    }

    public static String getMemberStatus(Member member) {
        if (hasDbots) {
            if (member.getGame().getType() == Game.GameType.TWITCH)
                return "<:streaming:313956277132853248>";

            switch (member.getOnlineStatus()) {
                case ONLINE: return "<:online:313956277808005120>";
                case IDLE: return "<:away:313956277220802560>";
                case DO_NOT_DISTURB: return "<:dnd:313956276893646850>";
                case OFFLINE: return "<:offline:313956277237710868>";
                case INVISIBLE: return "<:invisible:313956277107556352>";
                default: return "¯\\_(ツ)_/¯";
            }
        } else {
            return WordUtils.capitalizeFully(member.getOnlineStatus()
                    .name().replace('_', ' '));
        }
    }

    public static String getSuccess() {
        if (hasDbots)
            return "<:check:314349398811475968>";
        else
            return "✅";
    }

    public static String getFailure() {
        if (hasDbots)
            return "<:xmark:314349398824058880>";
        else
            return "❌";
    }

    public static String getCredits() {
        if (hasJda)
            return "<:jda:230988580904763393> by <:DV8:245233172189675520>, with <:idea:245257202305073152>";
        else
            return "JDA by DV8FromTheWorld, with IntelliJ IDEA";
    }

    public static String getGrave() {
        if (hasJda)
            return "<:rip:230989718471442432>";
        else
            return "⚰";
    }

    public static String getBotTag() {
        if (hasDbots)
            return "<:botTag:230105988211015680>";
        else
            return "[BOT]";
    }

    public static String getWarning() {
        return "⚠"; // don't have a custom one yet
    }
}
