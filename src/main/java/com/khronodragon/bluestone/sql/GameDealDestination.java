package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "gamedeal_dests")
public class GameDealDestination {
    @DatabaseField(id = true, canBeNull = false, index = true)
    private long channelId;

    @DatabaseField(canBeNull = false, index = true)
    private long guildId = 0L;

    @DatabaseField(canBeNull = false)
    private boolean steam = true;

    @DatabaseField(canBeNull = false)
    private boolean humbleBundle = true;

    @DatabaseField(canBeNull = false)
    private boolean origin = true;

    @DatabaseField(canBeNull = false)
    private short percentThreshold = 50;

    public GameDealDestination() {
    }

    public GameDealDestination(long channelId, long guildId, boolean steam, boolean humbleBundle, boolean origin, short percentThreshold) {
        this.channelId = channelId;
        this.guildId = guildId;
        this.steam = steam;
        this.humbleBundle = humbleBundle;
        this.origin = origin;
        this.percentThreshold = percentThreshold;
    }

    public boolean isSteam() {
        return steam;
    }

    public void setSteam(boolean steam) {
        this.steam = steam;
    }

    public boolean isHumbleBundle() {
        return humbleBundle;
    }

    public void setHumbleBundle(boolean humbleBundle) {
        this.humbleBundle = humbleBundle;
    }

    public boolean isOrigin() {
        return origin;
    }

    public void setOrigin(boolean origin) {
        this.origin = origin;
    }

    public short getPercentThreshold() {
        return percentThreshold;
    }

    public void setPercentThreshold(short percentThreshold) {
        this.percentThreshold = percentThreshold;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }
}
