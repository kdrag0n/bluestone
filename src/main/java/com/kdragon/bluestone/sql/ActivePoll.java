package com.kdragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "reaction_polls")
public class ActivePoll {
    @DatabaseField(id = true, canBeNull = false)
    private long messageId;

    @DatabaseField(canBeNull = false)
    private long channelId;

    @DatabaseField(canBeNull = false)
    private Date endTime;

    public ActivePoll() {
    }

    public ActivePoll(long messageId, long channelId, Date endTime) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.endTime = endTime;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getChannelId() {
        return channelId;
    }

    public Date getEndTime() {
        return endTime;
    }
}
