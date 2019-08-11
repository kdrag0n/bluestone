package com.kdrag0n.bluestone.modules;

import com.google.common.collect.ImmutableList;
import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.ShardedBot;
import com.kdrag0n.bluestone.annotations.EventHandler;
import com.zanox.lib.simplegraphiteclient.SimpleGraphiteClient;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.kdrag0n.bluestone.util.Strings.str;

public class StatReporterModule extends Module {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LoggerFactory.getLogger(StatReporterModule.class);
    private SimpleGraphiteClient graphiteClient;
    private static final AtomicInteger messagesSinceLastReport = new AtomicInteger();
    private static final AtomicInteger newGuildsSinceLastReport = new AtomicInteger();
    private static final String DISCORD_BOTS = "https://bots.discord.pw/api/bots/%s/stats";
    private static final String CARBONITEX = "https://www.carbonitex.net/discord/data/botdata.php";
    private static final String DISCORD_BOTS_ORG = "https://discordbots.org/api/bots/%s/stats";

    public StatReporterModule(Bot bot) {
        super(bot);

        if (bot.getConfig().has("graphite_host") && bot.getConfig().has("graphite_port")) {
            graphiteClient = new SimpleGraphiteClient(bot.getConfig().getString("graphite_host"),
                    bot.getConfig().getInt("graphite_port"));
            Bot.scheduledExecutor.scheduleAtFixedRate(this::graphiteReport, 2, 2, TimeUnit.MINUTES);
        }
    }

    public String getName() {
        return "Statistic Reporter";
    }

    public void onLoad() {
        report();
    }

    private void graphiteReport() {
        try {
            graphiteClient.sendMetrics(new HashMap<String, Number>() {
                {
                    Runtime runtime = Runtime.getRuntime();

                    put("bot.guilds", bot.getGuildCount());
                    put("bot.channels", bot.getChannelCount());
                    put("bot.voice_channels", bot.getVoiceChannelCount());
                    put("bot.text_channels", bot.getTextChannelCount());
                    put("bot.users", bot.getUserCount());
                    put("bot.shards", bot.manager.getShardsTotal());
                    put("bot.emotes", bot.getEmoteCount());
                    put("bot.music_tracks", bot.getTrackCount());
                    put("bot.music_streams", bot.getStreamCount());
                    put("bot.messages_per_min", messagesSinceLastReport.getAndSet(0));
                    put("bot.guilds_per_min", newGuildsSinceLastReport.getAndSet(0));

                    put("system.memory_used", runtime.totalMemory() - runtime.freeMemory());
                }
            });
        } catch (Exception e) {
            logger.error("Error reporting stats to Graphite", e);
        }
    }

    @EventHandler
    public void onMessageReceived(MessageReceivedEvent event) {
        messagesSinceLastReport.incrementAndGet();
    }

    @EventHandler
    public void onReady(ReadyEvent event) {
        report();
    }

    @EventHandler
    public void onGuildJoin(GuildJoinEvent event) {
        newGuildsSinceLastReport.incrementAndGet();

        report();
    }

    @EventHandler
    public void onGuildLeave(GuildLeaveEvent event) {
        newGuildsSinceLastReport.decrementAndGet();

        report();
    }

    private void report() {
        String dbotsKey = bot.getKeys().optString("discord_bots", null);
        String dBotListKey = bot.getKeys().optString("discord_bots_org", null);

        if (dbotsKey != null) {
            java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies")
                    .setLevel(Level.OFF);

            reportDiscordBots(dbotsKey);
        }

        if (dBotListKey != null)
            reportDBL(dBotListKey);
    }

    private List<JSONObject> constructJson() {
        if (bot.manager.getShardsTotal() > 1) {
            LinkedList<JSONObject> objects = new LinkedList<>();

            for (JDA shard : bot.manager.getShards()) {
                JDA.ShardInfo sInfo = shard.getShardInfo();

                JSONObject object = new JSONObject();
                object.put("shard_id", sInfo.getShardId())
                        .put("shard_count", bot.manager.getShardsTotal())
                        .put("server_count", shard.getGuilds().size());

                objects.add(object);
            }

            return objects;
        } else {
            JSONObject object = new JSONObject();
            object.put("server_count", bot.getGuildCount());
            return ImmutableList.of(object);
        }
    }
    private void reportDiscordBots(String key) {
        for (JSONObject object : constructJson()) {
            bot.http.newCall(new Request.Builder().post(RequestBody.create(JSON_MEDIA_TYPE, object.toString()))
                    .url(String.format(DISCORD_BOTS, bot.selfUser.getId())).header("Authorization", key).build())
                    .enqueue(Bot.callback(response -> {
                        if (!response.isSuccessful()) {
                            logger.warn("[Discord Bots] Bad response: {} {}", response.code(), response.message());
                        }
                    }, e -> {
                        if (e instanceof SocketTimeoutException)
                            logger.error("[Discord Bots] Report: timeout");
                        else
                            logger.error("[Discord Bots] Report failed", e);
                    }));
        }
    }

    private void reportDBL(String key) {
        for (JSONObject object : constructJson()) {
            bot.http.newCall(new Request.Builder().post(RequestBody.create(JSON_MEDIA_TYPE, object.toString()))
                    .url(String.format(DISCORD_BOTS_ORG, bot.selfUser.getId())).header("Authorization", key)
                    .build()).enqueue(Bot.callback(response -> {
                        if (!response.isSuccessful()) {
                            logger.warn("[Discord Bot List] Bad response: {} {}", response.code(), response.message());
                        }
                    }, e -> {
                        if (e instanceof SocketTimeoutException)
                            logger.error("[Discord Bot List] Report: timeout");
                        else
                            logger.error("[Discord Bot List] Report failed", e);
                    }));
        }
    }
}
