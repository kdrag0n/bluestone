package com.kdragon.bluestone;

import com.kdragon.bluestone.enums.MemberStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;
import org.apache.commons.text.WordUtils;

public class Emotes {
    private static boolean hasDbots = false;
    private static boolean hasParadise = false;

    static void setHasDbots() {
        Emotes.hasDbots = true;
    }

    static void setHasParadise(boolean hasParadise) {
        Emotes.hasParadise = hasParadise;
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
            if (member.getGame() != null && member.getGame().getType() == Game.GameType.STREAMING)
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

    public static String getStatusWithText(MemberStatus status) {
        if (hasDbots) {
            switch (status) {
                case STREAMING: return "<:streaming:313956277132853248> Streaming";
                case ONLINE: return "<:online:313956277808005120> Online";
                case IDLE: return "<:away:313956277220802560> Away";
                case DO_NOT_DISTURB: return "<:dnd:313956276893646850> Do Not Disturb";
                case OFFLINE: return "<:offline:313956277237710868> Offline";
                case INVISIBLE: return "<:invisible:313956277107556352> Invisible";
                default: return "¯\\_(ツ)_/¯";
            }
        } else {
            return WordUtils.capitalizeFully(status.name().replace('_', ' '));
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

    public static String getGrave() {
        if (hasParadise)
            return "<:rip:337405147347025930>";
        else
            return "⚰";
    }

    public static String getBotTag() {
        if (hasDbots)
            return "<:botTag:230105988211015680>";
        else
            return "[BOT]";
    }

    public static String getPlus() {
        if (hasParadise)
            return "<:plus:331224997362139136>";
        else
            return "➕";
    }
}
