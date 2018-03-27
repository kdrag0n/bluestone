package com.kdragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "note_settings")
class GuildNoteSettings {
    @DatabaseField(id = true, canBeNull = false, index = true)
    public long guildID;

    @DatabaseField(canBeNull = false)
    public boolean locked;

    public GuildNoteSettings() {
    }
}
