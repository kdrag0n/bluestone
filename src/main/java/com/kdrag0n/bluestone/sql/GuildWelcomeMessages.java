package com.kdrag0n.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "guild_welcome_msgs")
public class GuildWelcomeMessages {
    @DatabaseField(id = true, canBeNull = false, index = true)
    private long guildId;

    @DatabaseField(canBeNull = false)
    private long channelId = 0L;

    @DatabaseField(defaultValue = "[default]", width = 2000, canBeNull = false)
    private String welcome;

    @DatabaseField(defaultValue = "[default]", width = 2000, canBeNull = false)
    private String leave;

    @DatabaseField(canBeNull = false)
    private boolean welcomeEnabled = true;

    @DatabaseField(canBeNull = false)
    private boolean leaveEnabled = true;

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public String getWelcome() {
        return welcome;
    }

    public String getLeave() {
        return leave;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
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

    public boolean isWelcomeEnabled() {
        return welcomeEnabled;
    }

    public boolean isLeaveEnabled() {
        return leaveEnabled;
    }

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
