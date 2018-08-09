package com.kdragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "notes")
class GuildNote {
    @DatabaseField(canBeNull = false, width = 3, index = true)
    private short noteID;

    @DatabaseField(canBeNull = false, index = true)
    private long guildID;

    @DatabaseField(width = 360, canBeNull = false)
    private String text;

    @DatabaseField(canBeNull = false)
    private Date creationTime;

    @DatabaseField(canBeNull = false, index = true)
    private long authorID;

    public GuildNote() {
    }

    public GuildNote(short noteID, long guildID, String text, Date creationTime, long authorID) {
        this.noteID = noteID;
        this.guildID = guildID;
        this.text = text;
        this.creationTime = creationTime;
        this.authorID = authorID;
    }
}
