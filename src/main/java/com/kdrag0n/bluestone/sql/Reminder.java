package com.kdrag0n.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "reminders")
public class Reminder {
    @DatabaseField(generatedId = true, canBeNull = false)
    private int id;

    @DatabaseField(canBeNull = false)
    private long userId;

    @DatabaseField(width = 2000, canBeNull = false)
    private String message;

    @DatabaseField(canBeNull = false)
    private Date remindAt;

    public Reminder() {}

    public Reminder(long userId, String message, Date remindAt) {
        this.userId = userId;
        this.message = message;
        this.remindAt = remindAt;
    }

    public long getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public Date getRemindAt() {
        return remindAt;
    }
}
