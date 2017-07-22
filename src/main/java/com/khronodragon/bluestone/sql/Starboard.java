package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.Instant;
import java.util.Date;

@DatabaseTable(tableName = "starboards")
public class Starboard {
    @DatabaseField(id = true, canBeNull = false)
    private long guildId;

    @DatabaseField(canBeNull = false, index = true)
    private long channelId;

    @DatabaseField(canBeNull = false, width = 2)
    private int starThreshold = 1;

    @DatabaseField(canBeNull = false)
    private boolean locked = false;

    @DatabaseField(canBeNull = false)
    private Date maxAge = Date.from(Instant.ofEpochSecond(604800));

    public Starboard() {
    }

    public Starboard(long channelId, long guildId, int starThreshold, boolean locked) {
        this.channelId = channelId;
        this.guildId = guildId;
        this.starThreshold = starThreshold;
        this.locked = locked;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public int getStarThreshold() {
        return starThreshold;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Date getMaxAge() {
        return maxAge;
    }

    public void setStarThreshold(int starThreshold) {
        this.starThreshold = starThreshold;
    }

    public void setMaxAge(Date maxAge) {
        this.maxAge = maxAge;
    }
}
