package com.kdrag0n.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "music_settings")
public class GuildMusicSettings {
    @DatabaseField(id = true, canBeNull = false, index = true)
    private long id;

    @DatabaseField(canBeNull = false)
    private boolean alwaysPlayFirstResult;

    public GuildMusicSettings() {
    }

    public GuildMusicSettings(long id, boolean alwaysPlayFirstResult) {
        this.id = id;
        this.alwaysPlayFirstResult = alwaysPlayFirstResult;
    }

    public boolean alwaysPlayFirstResult() {
        return alwaysPlayFirstResult;
    }

    public void setAlwaysPlayFirstResult(boolean alwaysPlayFirstResult) {
        this.alwaysPlayFirstResult = alwaysPlayFirstResult;
    }
}
