package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "GuildNotesBannedMember")
class GuildNotesBannedMember {
    @DatabaseField(id = true, canBeNull = false, index = true)
    private long id;

    public GuildNotesBannedMember() {
    }

    public GuildNotesBannedMember(long id) {
        this.id = id;
    }
}
