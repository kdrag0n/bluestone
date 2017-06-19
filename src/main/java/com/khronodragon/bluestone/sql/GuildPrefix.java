package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "guild_prefixes")
public class GuildPrefix {
    @DatabaseField(id = true, canBeNull = false)
    private long guildId;

    public long getGuildId() {
        return guildId;
    }

    public String getPrefix() {
        return prefix;
    }

    @DatabaseField(defaultValue = "!", width = 32)
    private String prefix;

    public GuildPrefix() {}

    public GuildPrefix(long id, String pre) {
        guildId = id;
        prefix = pre;
    }
}
