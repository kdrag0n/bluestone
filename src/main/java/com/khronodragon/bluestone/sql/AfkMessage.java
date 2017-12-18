package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "afk_messages")
public class AfkMessage {
    @DatabaseField(id = true, canBeNull = false)
    private long userId;

    @DatabaseField(width = 150, canBeNull = false)
    private String message;

    public AfkMessage() {
    }

    public AfkMessage(long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
