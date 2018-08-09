package com.kdrag0n.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "starrers")
public class Starrer {
    @DatabaseField(generatedId = true, canBeNull = false)
    private long id;

    @DatabaseField(canBeNull = false, index = true)
    private long guildId;

    @DatabaseField(canBeNull = false, index = true)
    private long userId;

    @DatabaseField(canBeNull = false, index = true)
    private long messageId;

    public Starrer() {
    }

    public Starrer(long guildId, long userId, long messageId) {
        this.guildId = guildId;
        this.userId = userId;
        this.messageId = messageId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getUserId() {
        return userId;
    }

    public long getMessageId() {
        return messageId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Starrer && ((Starrer) other).getUserId() == userId;
    }
}
