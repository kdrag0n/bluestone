package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "GuildStory")
public class GuildStory {
    @DatabaseField(id = true, canBeNull = false)
    private long id;

    public GuildStory() {
    }

    public GuildStory(long id) {
        this.id = id;
    }
}
