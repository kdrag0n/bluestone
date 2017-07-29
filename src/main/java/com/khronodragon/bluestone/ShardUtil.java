package com.khronodragon.bluestone;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.DatabaseTypeUtils;
import com.j256.ormlite.db.MysqlDatabaseType;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.cogs.MusicCog;
import com.khronodragon.bluestone.sql.BotAdmin;
import com.khronodragon.bluestone.sql.GuildPrefix;
import com.khronodragon.bluestone.sql.MySQLDatabaseType;
import com.zaxxer.hikari.HikariDataSource;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ShardUtil {
    private static final Logger logger = LogManager.getLogger(ShardUtil.class);
    private static final MySQLDatabaseType mysqlDbType = new MySQLDatabaseType();
    private final Map<Integer, Bot> shards = new LinkedHashMap<>();
    public final Date startTime = new Date();
    private int shardCount;
    private Map<String, AtomicInteger> commandCalls = new HashMap<>();
    private Dao<BotAdmin, Long> adminDao;
    private ConnectionSource dbConn;
    private JSONObject config;
    private PrefixStore prefixStore;

    public PrefixStore getPrefixStore() {
        return prefixStore;
    }

    public Map<String, AtomicInteger> getCommandCalls() {
        return commandCalls;
    }

    ShardUtil(int shardCount, JSONObject config) {
        this.shardCount = shardCount;
        this.config = config;

        String connectionUrl = "jdbc:" + config.optString("db_url", "h2:./database");
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(connectionUrl);
        dataSource.setUsername(config.optString("db_user", null));
        dataSource.setPassword(config.optString("db_pass", null));
        dataSource.setMinimumIdle(5);
        dataSource.setMaximumPoolSize(15);
        dataSource.setPoolName("Bot Pool [ShardUtil]");
        dataSource.setAllowPoolSuspension(true);
        dataSource.setRegisterMbeans(true);
        dataSource.setLeakDetectionThreshold(7500);

        if (connectionUrl.startsWith("mysql://")) {
            dataSource.addDataSourceProperty("cachePrepStmts", "true");
            dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
            dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }

        DatabaseType dbType = DatabaseTypeUtils.createDatabaseType(connectionUrl);
        if (dbType instanceof MysqlDatabaseType)
            dbType = mysqlDbType;

        try {
            dbConn = new DataSourceConnectionSource(dataSource, dbType);
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dbConn.close();
            } catch (IOException e) {
                logger.warn("Couldn't close database connection", e);
            }
        }));
    }

    public Dao<BotAdmin, Long> getAdminDao() {
        return adminDao;
    }

    public ConnectionSource getDatabase() {
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

    public Collection<Bot> getShards() {
        return shards.values();
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

    public int getVoiceChannelCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getVoiceChannels().size()).sum();
    }

    public int getTextChannelCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getTextChannels().size()).sum();
    }

    public int getUserCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getUsers().size()).sum();
    }

    public int getRequestCount() {
        return commandCalls.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int getEmoteCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getEmotes().size()).sum();
    }

    public int getTrackCount() {
        return shards.values().stream().mapToInt(b -> {
            MusicCog cog = (MusicCog) b.cogs.get("Music");
            if (cog == null)
                return 0;

            return cog.getTracksLoaded();
        }).sum();
    }

    public int getStreamCount() {
        return shards.values().stream().mapToInt(b -> {
            MusicCog cog = (MusicCog) b.cogs.get("Music");
            if (cog == null)
                return 0;

            return cog.getActiveStreamCount();
        }).sum();
    }

    public Stream<Guild> getGuildStream() {
        return shards.values().stream().flatMap(b -> b.getJda().getGuilds().stream());
    }
}
