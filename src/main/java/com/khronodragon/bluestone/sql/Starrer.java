package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "starrers")
public class Starrer {
    @DatabaseField(generatedId = true, canBeNull = false)
    private long id;

    @DatabaseField(canBeNull = false, index = true)
    private long userId;

    @DatabaseField(canBeNull = false)
    private long entryMessageId;

    public Starrer() {
    }

    public Starrer(long userId, long entryMessageId) {
        this.userId = userId;
        this.entryMessageId = entryMessageId;
    }

    public long getUserId() {
        return userId;
    }

    public long getEntryMessageId() {
        return entryMessageId;
    }
}
