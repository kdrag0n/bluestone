package com.khronodragon.bluestone.sql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "guild_prefixes")
public class GuildPrefix {
    @Id
    @Column(nullable = false)
    private long guildId;

    @Column(length = 32, nullable = false)
    private String prefix;

    public long getGuildId() {
        return guildId;
    }

    public String getPrefix() {
        return prefix;
    }

    public GuildPrefix() {}

    public GuildPrefix(long id, String pre) {
        guildId = id;
        prefix = pre;
    }
}
