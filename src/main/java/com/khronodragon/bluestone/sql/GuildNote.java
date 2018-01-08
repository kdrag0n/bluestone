package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "notes")
public class GuildNote {
    @DatabaseField(canBeNull = false, width = 3, index = true)
    public short noteID;

    @DatabaseField(canBeNull = false, index = true)
    public long guildID;

    @DatabaseField(width = 360, canBeNull = false)
    public String text;

    @DatabaseField(canBeNull = false)
    public Date creationTime;

    @DatabaseField(canBeNull = false, index = true)
    public long authorID;

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
