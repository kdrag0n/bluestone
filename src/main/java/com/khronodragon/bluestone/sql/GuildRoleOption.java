package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Permissions;

import java.time.OffsetDateTime;

import static com.khronodragon.bluestone.sql.GuildRoleOption.RoleConditions.*;

@DatabaseTable(tableName = "role_options")
public class GuildRoleOption {
    @DatabaseField(id = true, canBeNull = false, index = true)
    private long roleId;

    @DatabaseField(canBeNull = false, index = true)
    private long guildId;

    @DatabaseField(canBeNull = false)
    private int conditions = 0;

    @DatabaseField(canBeNull = false, width = 6000, defaultValue = "{}")
    private String extraData;

    public GuildRoleOption() {
    }

    public GuildRoleOption(long roleId, long guildId, int conditions, String extraData) {
        this.roleId = roleId;
        this.guildId = guildId;
        this.conditions = conditions;
        this.extraData = extraData;
    }

    public long getRoleId() {
        return roleId;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getConditions() {
        return conditions;
    }

    public String getExtraData() {
        return extraData;
    }

    public boolean test(Context ctx, int mSend, boolean hasBeenMentioned, boolean hasMentionedOther) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime mJoin = ctx.member.getJoinDate();

        // Join time
        if ((conditions & JOIN_1_MINUTE) == JOIN_1_MINUTE) {
            if (mJoin.isBefore(now.minusMinutes(1)))
                return false;
        }

        if ((conditions & JOIN_5_MINUTES) == JOIN_5_MINUTES) {
            if (mJoin.isBefore(now.minusMinutes(5)))
                return false;
        }

        if ((conditions & JOIN_10_MINUTES) == JOIN_10_MINUTES) {
            if (mJoin.isBefore(now.minusMinutes(10)))
                return false;
        }

        if ((conditions & JOIN_15_MINUTES) == JOIN_15_MINUTES) {
            if (mJoin.isBefore(now.minusMinutes(15)))
                return false;
        }

        // Sending messages
        if ((conditions & SEND_1_MESSAGE) == SEND_1_MESSAGE) {
            if (mSend < 1)
                return false;
        }

        if ((conditions & SEND_2_MESSAGES) == SEND_2_MESSAGES) {
            if (mSend < 2)
                return false;
        }

        if ((conditions & SEND_5_MESSAGES) == SEND_5_MESSAGES) {
            if (mSend < 5)
                return false;
        }

        if ((conditions & SEND_10_MESSAGES) == SEND_10_MESSAGES) {
            if (mSend < 10)
                return false;
        }

        // Special
        if ((conditions & HAS_BEEN_MENTIONED) == HAS_BEEN_MENTIONED) {
            if (!hasBeenMentioned)
                return false;
        }

        if ((conditions & HAS_MENTIONED_OTHER) == HAS_MENTIONED_OTHER) {
            if (!hasMentionedOther)
                return false;
        }

        if ((conditions & HAS_CUSTOM_AVATAR) == HAS_CUSTOM_AVATAR) {
            if (ctx.member.getUser().getAvatarId() == null)
                return false;
        }

        // Ranks
        if ((conditions & IS_BOT_OWNER) == IS_BOT_OWNER) {
            if (!Permissions.check(ctx, Permissions.BOT_OWNER))
                return false;
        }

        if ((conditions & IS_BOT_ADMIN) == IS_BOT_ADMIN) {
            if (!Permissions.check(ctx, Permissions.BOT_ADMIN))
                return false;
        }

        if ((conditions & IS_PATRON) == IS_PATRON) {
            if (!Permissions.check(ctx, Permissions.PATREON_SUPPORTER))
                return false;
        }

        return true;
    }

    public class RoleConditions {
        // Join time
        public static final int JOIN_1_MINUTE = 1<<1;
        public static final int JOIN_5_MINUTES = 1<<2;
        public static final int JOIN_10_MINUTES = 1<<3;
        public static final int JOIN_15_MINUTES = 1<<4;

        // Sending messages
        public static final int SEND_1_MESSAGE = 1<<5;
        public static final int SEND_2_MESSAGES = 1<<6;
        public static final int SEND_5_MESSAGES = 1<<7;
        public static final int SEND_10_MESSAGES = 1<<8;

        // Special
        public static final int HAS_BEEN_MENTIONED = 1<<9;
        public static final int HAS_MENTIONED_OTHER = 1<<10;
        public static final int HAS_CUSTOM_AVATAR = 1<<11;

        // Ranks
        public static final int IS_BOT_OWNER = 1<<12;
        public static final int IS_BOT_ADMIN = 1<<13;
        public static final int IS_PATRON = 1<<14;
    }
}
