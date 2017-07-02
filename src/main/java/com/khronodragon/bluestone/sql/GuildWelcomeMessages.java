package com.khronodragon.bluestone.sql;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "guild_welcome_msgs")
public class GuildWelcomeMessages {
    @Id
    @Column(nullable = false)
    private long guildId;

    @ColumnDefault("[default]")
    @Column(length = 2000, nullable = false)
    private String welcome;

    @ColumnDefault("[default]")
    @Column(length = 2000, nullable = false)
    private String leave;

    @Column(nullable = false)
    private boolean welcomeEnabled;

    @Column(nullable = false)
    private boolean leaveEnabled;

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
