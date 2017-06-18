package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "admins")
public class BotAdmin {
    @DatabaseField(id = true)
    private long userId;

    public long getUserId() {
        return userId;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    @DatabaseField(canBeNull = true, defaultValue = "", width = 32)
    private String lastUsername;

    public BotAdmin() {}

    public BotAdmin(long uid, String uname) {
        userId = uid;
        lastUsername = uname;
    }
}
