package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "guild_welcome_msgs")
public class GuildWelcomeMessage {
    @DatabaseField(id = true, canBeNull = false)
    private long guildId;

    public long getGuildId() {
        return guildId;
    }

    public String getMessage() {
        return message;
    }

    @DatabaseField(defaultValue = "[default]", width = 2000, canBeNull = false)
    private String message;

    public GuildWelcomeMessage() {}

    public GuildWelcomeMessage(long id, String msg) {
        guildId = id;
        message = msg;
    }
}
