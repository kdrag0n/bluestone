package com.kdragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "GuildStory")
class GuildStory {
    @DatabaseField(id = true, canBeNull = false, index = true)
    private long id;

    public GuildStory() {
    }

    public GuildStory(long id) {
        this.id = id;
    }
}
