package com.kdrag0n.bluestone.types;

import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;

public enum MemberStatus {
    ONLINE,
    STREAMING,
    IDLE,
    DO_NOT_DISTURB,
    INVISIBLE,
    OFFLINE,
    UNKNOWN;

    private static MemberStatus from(OnlineStatus status) {
        switch (status) {
            case ONLINE: return ONLINE;
            case IDLE: return IDLE;
            case DO_NOT_DISTURB: return DO_NOT_DISTURB;
            case INVISIBLE: return INVISIBLE;
            case OFFLINE: return OFFLINE;
            default: return UNKNOWN;
        }
    }

    public static MemberStatus from(Member member) {
        if (member.getGame() != null && member.getGame().getType() == Game.GameType.STREAMING)
            return STREAMING;
        else
            return from(member.getOnlineStatus());
    }
}
