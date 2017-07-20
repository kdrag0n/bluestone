package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "ActivePoll")
public class ActivePoll {
    @DatabaseField(id = true, canBeNull = false)
    private long id;

    public ActivePoll() {
    }

    public ActivePoll(long id) {
        this.id = id;
    }
}
