package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "note_settings")
public class GuildNoteSettings {
    @DatabaseField(id = true, canBeNull = false, index = true)
    public long guildID;

    @DatabaseField(canBeNull = false)
    public boolean locked;

    public GuildNoteSettings() {
    }
}
