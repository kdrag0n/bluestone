package com.kdrag0n.bluestone;

import com.google.common.collect.Ordering;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.DatabaseTypeUtils;
import com.j256.ormlite.db.MysqlDatabaseType;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.kdrag0n.bluestone.sql.GuildPrefix;
import com.kdrag0n.bluestone.sql.MySQLDatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gnu.trove.list.TLongList;
import gnu.trove.list.linked.TLongLinkedList;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public abstract class ShardedBot {
    private static final Logger logger = LoggerFactory.getLogger(ShardedBot.class);
    private static final MySQLDatabaseType mysqlDbType = new MySQLDatabaseType();
    public final Date startTime = new Date();
    private ConnectionSource dbConn;
    private HikariDataSource dataSource;
    private JSONObject config;
    public final ShardManager manager;

    ShardedBot(ShardManager manager, JSONObject config) {
        this.manager = manager;
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
            logger.error("Failed to connect to database!", e);
            System.exit(1);
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

    /*package-private*/ HikariDataSource getPool() {
        return dataSource;
    }

    public JSONObject getConfig() {
        return config;
    }

    public List<JDA> getSortedShards() {
        return Ordering.from(Comparator.comparingInt((JDA shard) -> shard.getShardInfo().getShardId()))
                .sortedCopy(manager.getShards());
    }

    public long getGuildCount() {
        return sumJda(j -> j.getGuildCache().size());
    }

    public long getTotalChannelCount() {
        return sumJda(jda -> jda.getTextChannelCache().size() + jda.getVoiceChannelCache().size());
    }

    public long getVoiceChannelCount() {
        return sumJda(j -> j.getVoiceChannelCache().size());
    }

    public long getTextChannelCount() {
        return sumJda(j -> j.getTextChannelCache().size());
    }

    public long getUserCount() {
        return sumJda(j -> j.getUserCache().size());
    }

    public long getEmoteCount() {
        return sumJda(j -> j.getEmoteCache().size());
    }

    private long sumJda(ObjectFunctionLong<JDA> fn) {
        long total = 0;

        for (JDA shard : manager.getShards()) {
            total += fn.apply(shard);
        }

        return total;
    }

    public TLongList guildNums(ObjectFunctionLong<Guild> fn) {
        TLongList l = new TLongLinkedList();

        for (JDA shard : manager.getShards()) {
            for (Guild guild : shard.getGuildCache()) {
                l.add(fn.apply(guild));
            }
        }

        return l;
    }

    public int guildCount(ObjectFunctionBool<Guild> fn) {
        int count = 0;

        for (JDA shard : manager.getShards()) {
            for (Guild guild : shard.getGuildCache()) {
                if (fn.apply(guild))
                    count++;
            }
        }

        return count;
    }

    @FunctionalInterface
    public interface ObjectFunctionLong<A> {
        long apply(A value);
    }

    @FunctionalInterface
    public interface ObjectFunctionBool<A> {
        boolean apply(A value);
    }
}
