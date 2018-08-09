package com.kdragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "autoroles")
public class GuildAutorole {
    @DatabaseField(id = true, canBeNull = false)
    private long roleId;

    @DatabaseField(canBeNull = false, index = true)
    private long guildId;

    @DatabaseField(canBeNull = false)
    private int conditions = 0;

    @DatabaseField(canBeNull = false, width = 6000, defaultValue = "{}")
    private String extraData;

    public GuildAutorole() {
    }

    public GuildAutorole(long roleId, long guildId, int conditions, String extraData) {
        this.roleId = roleId;
        this.guildId = guildId;
        this.conditions = conditions;
        this.extraData = extraData;
    }

    public long getRoleId() {
        return roleId;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getConditions() {
        return conditions;
    }

    public String getExtraData() {
        return extraData;
    }
}
