package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "quotes_banned_members")
public class QuotesBannedMember {
    @DatabaseField(id = true, canBeNull = false)
    public long id;

    public QuotesBannedMember() {
    }

    public QuotesBannedMember(long id) {
        this.id = id;
    }
}
