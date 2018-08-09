package com.kdragon.bluestone;

import com.zaxxer.hikari.HikariDataSource;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrefixStore {
    private static final Logger logger = LogManager.getLogger(PrefixStore.class);
    public final String defaultPrefix;
    private final HikariDataSource pool;
    public final TLongObjectMap<String> cache = new TLongObjectHashMap<>();

    PrefixStore(HikariDataSource pool, String defaultPrefix) {
        this.pool = pool;
        this.defaultPrefix = defaultPrefix;
    }

    public String getPrefix(long guildId) {
        String prefix = cache.get(guildId);

        if (prefix != null) {
            return prefix;
        } else {
            try (Connection conn = pool.getConnection()) {
                ResultSet result = conn.createStatement()
                        .executeQuery("SELECT prefix FROM guild_prefixes WHERE guildId = " + guildId + ';');
                if (result.first()) {
                    prefix = result.getString(1);
                    cache.put(guildId, prefix);
                    return prefix;
                }

                cache.put(guildId, defaultPrefix);
                return defaultPrefix;
            } catch (SQLException|NullPointerException e) {
                logger.error("Error getting prefix from DB", e);
                return defaultPrefix;
            }
        }
    }
}
