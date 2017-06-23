package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "guild_welcome_msgs")
public class GuildWelcomeMessages {
    @DatabaseField(id = true, canBeNull = false)
    private long guildId;

    public long getGuildId() {
        return guildId;
    }

    public String getWelcome() {
        return welcome;
    }

    public String getLeave() {
        return leave;
    }

    public void setWelcome(String welcome) {
        this.welcome = welcome;
    }

    public void setLeave(String leave) {
        this.leave = leave;
    }

    public void setWelcomeEnabled(boolean welcomeEnabled) {
        this.welcomeEnabled = welcomeEnabled;
    }

    public void setLeaveEnabled(boolean leaveEnabled) {
        this.leaveEnabled = leaveEnabled;
    }

    @DatabaseField(defaultValue = "[default]", width = 2000, canBeNull = false)
    private String welcome;

    @DatabaseField(defaultValue = "[default]", width = 2000, canBeNull = false)
    private String leave;

    public boolean isWelcomeEnabled() {
        return welcomeEnabled;
    }

    public boolean isLeaveEnabled() {
        return leaveEnabled;
    }

    @DatabaseField(canBeNull = false)
    private boolean welcomeEnabled;

    @DatabaseField(canBeNull = false)
    private boolean leaveEnabled;

    public GuildWelcomeMessages() {}

    public GuildWelcomeMessages(long id, String welcome, String leave, boolean welcomeEnabled,
                                boolean leaveEnabled) {
        guildId = id;
        this.welcome = welcome;
        this.leave = leave;
        this.welcomeEnabled = welcomeEnabled;
        this.leaveEnabled = leaveEnabled;
    }
}
