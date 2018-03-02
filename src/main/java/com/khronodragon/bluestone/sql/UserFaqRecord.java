package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "user_faq_records")
public class UserFaqRecord {
    @DatabaseField(id = true, canBeNull = false)
    private long userId;

    @DatabaseField(canBeNull = false)
    private boolean hasReadFaq;

    @DatabaseField(canBeNull = false)
    public Date when;

    public UserFaqRecord() {}

    public UserFaqRecord(long userId, boolean hasReadFaq, Date when) {
        this.userId = userId;
        this.hasReadFaq = hasReadFaq;
        this.when = when;
    }
}
