package com.kdrag0n.bluestone.modules;

import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.annotations.EventHandler;
import com.zanox.lib.simplegraphiteclient.SimpleGraphiteClient;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class StatReporterModule extends Module {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LoggerFactory.getLogger(StatReporterModule.class);
    private SimpleGraphiteClient graphiteClient;
    private final AtomicInteger messagesSinceLastReport = new AtomicInteger();
    private final AtomicInteger newGuildsSinceLastReport = new AtomicInteger();
    private static final String BOD_URL = "https://bots.ondiscord.xyz/bot-api/bots/%s/guilds";
    private static final String DBL_URL = "https://discordbots.org/api/bots/%s/stats";

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
                    put("bot.channels", bot.getTotalChannelCount());
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
        String bodKey = bot.getKeys().optString("bots_on_discord", null);
        String dblKey = bot.getKeys().optString("discord_bot_list", null);

        if (bodKey != null) {
            java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies")
                    .setLevel(Level.OFF);

            reportBod(bodKey);
        }

        if (dblKey != null)
            reportDbl(dblKey);
    }

    private JSONObject constructBodJson() {
        JSONObject object = new JSONObject();
        object.put("guildCount", bot.getGuildCount());
        return object;
    }

    private JSONObject constructDblJson() {
        JSONObject object = new JSONObject();

        if (bot.manager.getShardsTotal() > 1) {
            ArrayList<Integer> shardCounts = new ArrayList<>(bot.manager.getShardsTotal());

            for (JDA shard : bot.getSortedShards())
                shardCounts.add(shard.getGuilds().size());

            object.put("shard_count", bot.manager.getShardsTotal());
            object.put("shards", shardCounts);
        } else {
            object.put("server_count", bot.getGuildCount());
        }

        return object;
    }

    private void reportBod(String key) {
        JSONObject object = constructBodJson();

        bot.http.newCall(new Request.Builder().post(RequestBody.create(JSON_MEDIA_TYPE, object.toString()))
                .url(String.format(BOD_URL, bot.selfUser.getId())).header("Authorization", key).build())
                .enqueue(Bot.callback(response -> {
                    if (!response.isSuccessful()) {
                        logger.warn("[Bots on Discord] Bad response: {} {}", response.code(), response.message());
                    }
                }, e -> {
                    if (e instanceof SocketTimeoutException)
                        logger.error("[Bots on Discord] Report: timeout");
                    else
                        logger.error("[Bots on Discord] Report failed", e);
                }));
    }

    private void reportDbl(String key) {
        JSONObject object = constructDblJson();

        bot.http.newCall(new Request.Builder().post(RequestBody.create(JSON_MEDIA_TYPE, object.toString()))
                .url(String.format(DBL_URL, bot.selfUser.getId())).header("Authorization", key)
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
