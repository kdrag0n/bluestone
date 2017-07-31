package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "user_profiles")
public class UserProfile {
    @DatabaseField(id = true, canBeNull = false)
    private long userId;

    @DatabaseField(canBeNull = false)
    private int flags;

    @DatabaseField(defaultValue = "[]", width = 8000)
    private String questionValues;

    public UserProfile() {
    }

    public UserProfile(long userId, int flags, String questionValues) {
        this.userId = userId;
        this.flags = flags;
        this.questionValues = questionValues;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getQuestionValues() {
        return questionValues;
    }

    public void setQuestionValues(String questionValues) {
        this.questionValues = questionValues;
    }
}
