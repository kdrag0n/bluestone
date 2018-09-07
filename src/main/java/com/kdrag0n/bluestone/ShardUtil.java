package com.kdrag0n.bluestone;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.DatabaseTypeUtils;
import com.j256.ormlite.db.MysqlDatabaseType;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.kdrag0n.bluestone.cogs.MusicCog;
import com.kdrag0n.bluestone.sql.GuildPrefix;
import com.kdrag0n.bluestone.sql.MySQLDatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ShardUtil {
    private static final Logger logger = LoggerFactory.getLogger(ShardUtil.class);
    private static final MySQLDatabaseType mysqlDbType = new MySQLDatabaseType();
    private final Map<Integer, Bot> shards = new LinkedHashMap<>();
    public final Date startTime = new Date();
    private int shardCount;
    private ConnectionSource dbConn;
    private HikariDataSource dataSource;
    private JSONObject config;

    ShardUtil(int shardCount, JSONObject config) {
        this.shardCount = shardCount;
        this.config = config;

        String connectionUrl = "jdbc:" + config.optString("db_url", "h2:./database");
        HikariConfig dbConfig = new HikariConfig();
        dbConfig.setJdbcUrl(connectionUrl);
        dbConfig.setUsername(config.optString("db_user", null));
        dbConfig.setPassword(config.optString("db_pass", null));
        dbConfig.setMinimumIdle(5);
        dbConfig.setMaximumPoolSize(15);
        dbConfig.setPoolName("Bot");
        dbConfig.setAllowPoolSuspension(true);
        dbConfig.setRegisterMbeans(true);
        dbConfig.setLeakDetectionThreshold(7500);

        if (connectionUrl.startsWith("mysql://")) {
            dbConfig.addDataSourceProperty("cachePrepStmts", "true");
            dbConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            dbConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        }

        dataSource = new HikariDataSource(dbConfig);

        DatabaseType dbType = DatabaseTypeUtils.createDatabaseType(connectionUrl);
        if (dbType instanceof MysqlDatabaseType)
            dbType = mysqlDbType;

        try {
            dbConn = new DataSourceConnectionSource(dataSource, dbType);
        } catch (SQLException e) {
            logger.error("Failed to connect to database! Falling back to in-memory.", e);

            try {
                dbConn = new JdbcConnectionSource("jdbc:h2:mem:bluestone-db");
            } catch (SQLException ex) {
                logger.error("Failed to create in-memory database!", ex);
                System.exit(-1);
            }
        }

        try {
            TableUtils.createTableIfNotExists(dbConn, GuildPrefix.class);
        } catch (SQLException e) {
            logger.error("Failed to create command prefix table!", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dbConn.close();
            } catch (IOException e) {
                logger.warn("Couldn't close database connection", e);
            }
        }));
    }

    @CheckReturnValue
    public <C, K> Dao<C, K> setupDao(Class<C> clazz) {
        try {
            TableUtils.createTableIfNotExists(dbConn, clazz);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating table for " + clazz.getSimpleName() + '!', e);
        }

        try {
            return DaoManager.createDao(dbConn, clazz);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating DAO for " + clazz.getSimpleName() + '!', e);
        }
    }

    public ConnectionSource getDatabase() {
        return dbConn;
    }

    public HikariDataSource getPool() {
        return dataSource;
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
        return sumJda(j -> j.getGuildMap().size());
    }

    public int getChannelCount() {
        return sumJda(jda -> jda.getTextChannelMap().size() + jda.getVoiceChannelMap().size());
    }

    public int getVoiceChannelCount() {
        return sumJda(j -> j.getVoiceChannelMap().size());
    }

    public int getTextChannelCount() {
        return sumJda(j -> j.getTextChannelMap().size());
    }

    public int getUserCount() {
        return sumJda(j -> j.getUserMap().size());
    }

    public int getEmoteCount() {
        return sumJda(j -> (int) j.getEmoteCache().size());
    }

    public int getTrackCount() {
        return sumBot(b -> {
            MusicCog cog = (MusicCog) b.cogs.get("Music");
            if (cog == null)
                return 0;

            return cog.getTracksLoaded();
        });
    }

    public int getStreamCount() {
        return sumBot(b -> {
            MusicCog cog = (MusicCog) b.cogs.get("Music");
            if (cog == null)
                return 0;

            return cog.getActiveStreamCount();
        });
    }

    @Deprecated
    public Stream<Guild> getGuildStream() {
        return shards.values().stream().flatMap(b -> ((JDAImpl) b.jda).getGuildMap().valueCollection().stream());
    }

    private int sumBot(ObjectFunctionInt<Bot> fn) {
        int total = 0;

        for (Bot shard : shards.values()) {
            total += fn.apply(shard);
        }

        return total;
    }

    private int sumJda(ObjectFunctionInt<JDAImpl> fn) {
        int total = 0;

        for (Bot shard : shards.values()) {
            total += fn.apply((JDAImpl) shard.jda);
        }

        return total;
    }

    public TIntList guildNums(ObjectFunctionInt<GuildImpl> fn) {
        TIntList l = new TIntLinkedList();

        for (Bot shard : shards.values()) {
            for (Guild guild : shard.jda.getGuildCache()) {
                l.add(fn.apply((GuildImpl) guild));
            }
        }

        return l;
    }

    public int guildCount(ObjectFunctionBool<GuildImpl> fn) {
        int c = 0;

        for (Bot shard : shards.values()) {
            for (Guild guild : shard.jda.getGuildCache()) {
                if (fn.apply((GuildImpl) guild))
                    c++;
            }
        }

        return c;
    }

    @FunctionalInterface
    public interface ObjectFunctionInt<A> {
        int apply(A value);
    }

    @FunctionalInterface
    public interface ObjectFunctionBool<A> {
        boolean apply(A value);
    }
}
