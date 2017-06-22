package com.khronodragon.bluestone;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.sql.BotAdmin;
import com.khronodragon.bluestone.sql.GuildPrefix;
import net.dv8tion.jda.core.JDA;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ShardUtil {
    private static final Logger logger = LogManager.getLogger(ShardUtil.class);
    private Map<Integer, Bot> shards = new LinkedHashMap<>();
    private int shardCount;

    private Dao<BotAdmin, Long> adminDao;
    private JdbcConnectionSource dbConn;
    private JSONObject config;

    public PrefixStore getPrefixStore() {
        return prefixStore;
    }

    private PrefixStore prefixStore;

    ShardUtil(int shardCount, JSONObject config) {
        this.shardCount = shardCount;
        this.config = config;

        try {
            dbConn = new JdbcPooledConnectionSource("jdbc:" + config.optString("db_url", "h2:./database"));
        } catch (SQLException e) {
            logger.error("Failed to connect to database!", e);
            logger.warn("Using an in-memory database.");

            try {
                dbConn = new JdbcConnectionSource("jdbc:h2:mem:bluestone-db");
            } catch (SQLException ex) {
                logger.error("Failed to create in-memory database!", ex);
                System.exit(-1);
            }
        }

        try {
            TableUtils.createTableIfNotExists(dbConn, BotAdmin.class);
        } catch (SQLException e) {
            logger.warn("Failed to create bot admin table!", e);
        }

        try {
            adminDao = DaoManager.createDao(dbConn, BotAdmin.class);
        } catch (SQLException e) {
            logger.warn("Failed to create bot admin DAO!", e);
        }

        try {
            TableUtils.createTableIfNotExists(dbConn, GuildPrefix.class);
        } catch (SQLException e) {
            logger.warn("Failed to create command prefix table!", e);
        }

        try {
            Dao<GuildPrefix, Long> dao = DaoManager.createDao(dbConn, GuildPrefix.class);
            prefixStore = new PrefixStore(dao, config.optString("default_prefix", "!"));
        } catch (SQLException e) {
            logger.warn("Failed to create prefix store and/or DAO!", e);
        }
    }

    public Dao<BotAdmin, Long> getAdminDao() {
        return adminDao;
    }

    public JdbcConnectionSource getDatabase() {
        return dbConn;
    }

    public JSONObject getConfig() {
        return config;
    }

    public int getShardCount() {
        return shardCount;
    }

    public Bot getShard(int shardId) {
        return shards.get(shardId);
    }

    public void setShard(int shardId, Bot bot) {
        shards.put(shardId, bot);
    }

    public Set<Bot> getShards() {
        return new LinkedHashSet<>(shards.values());
    }

    public int getGuildCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getGuilds().size()).sum();
    }

    public int getChannelCount() {
        return shards.values().stream().mapToInt(b -> {
            JDA jda = b.getJda();
            return jda.getTextChannels().size() + jda.getVoiceChannels().size();
        }).sum();
    }

    public int getUserCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getUsers().size()).sum();
    }

    public int getRequestCount() {
        return shards.values().stream().mapToInt(b -> b.commandCalls.values().stream().mapToInt(AtomicInteger::get).sum()).sum();
    }


}
