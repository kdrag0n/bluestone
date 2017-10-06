package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Permissions;

import java.time.OffsetDateTime;
import java.util.function.Predicate;

import static com.khronodragon.bluestone.sql.GuildRoleOption.RoleConditions.*;

@DatabaseTable(tableName = "role_options")
public class GuildRoleOption implements Predicate<Context> {
    private static final String[] BOT_OWNER_PERM
    @DatabaseField(id = true, canBeNull = false)
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

    public boolean test(Context ctx) {
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

        }

        // Special

        // Ranks
        if ((conditions & IS_BOT_OWNER) == IS_BOT_OWNER) {
            if (!Permissions.check(BOT_OWNER_PERM, ctx))
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
        public static final int HAS_BOT_PROFILE = 1<<12;

        // Ranks
        public static final int IS_BOT_OWNER = 1<<13;
        public static final int IS_BOT_ADMIN = 1<<14;
        public static final int IS_PATRON = 1<<15;
    }
}
