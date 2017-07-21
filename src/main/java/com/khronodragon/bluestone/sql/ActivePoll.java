package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "reaction_polls")
public class ActivePoll {
    @DatabaseField(generatedId = true, canBeNull = false)
    private long id;

    @DatabaseField(canBeNull = false)
    private long messageId;

    public ActivePoll() {
    }

    public ActivePoll(long id) {
        this.id = id;
    }
}
