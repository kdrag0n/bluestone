package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "contact_banned_users")
public class ContactBannedUser {
    @DatabaseField(id = true, canBeNull = false)
    public long id;

    public ContactBannedUser() {
    }

    public ContactBannedUser(long id) {
        this.id = id;
    }
}
