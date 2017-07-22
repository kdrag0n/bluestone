package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "starboard_entries")
public class StarboardEntry {
    @DatabaseField(id = true, canBeNull = false)
    private long messageId;

    @DatabaseField(canBeNull = false, index = true)
    private long guildId;

    @DatabaseField(canBeNull = false)
    private long botMessageId;

    @DatabaseField(canBeNull = false)
    private long botChannelId;

    @DatabaseField(canBeNull = false)
    private long authorId;

    @DatabaseField(canBeNull = false)
    private long channelId;

    public StarboardEntry() {
    }

    public StarboardEntry(long messageId, long guildId, long botMessageId, long botChannelId, long authorId, long channelId) {
        this.messageId = messageId;
        this.guildId = guildId;
        this.botMessageId = botMessageId;
        this.botChannelId = botChannelId;
        this.authorId = authorId;
        this.channelId = channelId;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getBotMessageId() {
        return botMessageId;
    }

    public long getBotChannelId() {
        return botChannelId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public long getChannelId() {
        return channelId;
    }
}
