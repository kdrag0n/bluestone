package com.kdrag0n.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "afk_messages")
public class AfkMessage {
    @DatabaseField(id = true, canBeNull = false, index = true)
    private long userId;

    @DatabaseField(width = 150, canBeNull = false)
    private String message;

    public AfkMessage() {
    }

    public AfkMessage(long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public long getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }
}
