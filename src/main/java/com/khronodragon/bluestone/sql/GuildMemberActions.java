package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "member_actions")
public class GuildMemberActions {
    @DatabaseField(canBeNull = false, index = true)
    private long userId;

    @DatabaseField(canBeNull = false, index = true)
    private long guildId;

    @DatabaseField
    public short messagesSent = 0;

    @DatabaseField
    public boolean hasBeenMentioned = false;

    @DatabaseField
    public boolean hasMentionedOther = false;

    public GuildMemberActions() {
    }

    public GuildMemberActions(long userId, long guildId, short messagesSent, boolean hasBeenMentioned, boolean hasMentionedOther) {
        this.userId = userId;
        this.guildId = guildId;
        this.messagesSent = messagesSent;
        this.hasBeenMentioned = hasBeenMentioned;
        this.hasMentionedOther = hasMentionedOther;
    }
}
