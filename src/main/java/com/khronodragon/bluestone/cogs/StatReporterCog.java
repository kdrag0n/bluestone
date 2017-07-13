package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.annotations.EventHandler;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;

import static com.khronodragon.bluestone.util.Strings.str;

public class StatReporterCog extends Cog {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Logger logger = LogManager.getLogger(StatReporterCog.class);

    private enum Endpoints {
        DISCORD_BOTS("https://bots.discord.pw/api/bots/{0}/stats"),
        CARBONITEX("https://www.carbonitex.net/discord/data/botdata.php");

        private String endpoint;

        Endpoints(String endpoint) {
            this.endpoint = endpoint;
        }

        String format(Object... args) {
            return MessageFormat.format(endpoint, args);
        }

        String getUrl() {
            return endpoint;
        }
    }

    public StatReporterCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Statistic Reporter";
    }

    public String getDescription() {
        return "A cog to report bot stats to services like Discord Bots and Carbonitex.";
    }

    public void load() {
        super.load();
        report();
    }

    @EventHandler
    public void onReady(ReadyEvent event) {
        report();
    }

    @EventHandler
    public void onGuildJoin(GuildJoinEvent event) {
        report();
    }

    @EventHandler
    public void onGuildLeave(GuildLeaveEvent event) {
        report();
    }

    private void report() {
        String dbotsKey = bot.getKeys().optString("discord_bots", null);
        String carbonitexKey = bot.getKeys().optString("carbonitex", null);

        if (dbotsKey != null || carbonitexKey != null)
            java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies")
                    .setLevel(Level.OFF);

        if (dbotsKey != null)
            reportDiscordBots(dbotsKey);

        if (carbonitexKey != null)
            reportCarbonitex(carbonitexKey);
    }

    private void reportDiscordBots(String key) {
        JSONObject json = new JSONObject();
        if (bot.getJda().getShardInfo() != null) {
            JDA.ShardInfo sInfo = bot.getJda().getShardInfo();

            json.put("shard_id", sInfo.getShardId())
                    .put("shard_count", sInfo.getShardTotal())
                    .put("server_count", bot.getJda().getGuilds().size());
        } else {
            json.put("server_count", bot.getJda().getGuilds().size());
        }

        bot.http.newCall(new Request.Builder()
                .post(RequestBody.create(JSON_MEDIA_TYPE, json.toString()))
                .url(Endpoints.DISCORD_BOTS.format(bot.getJda().getSelfUser().getId()))
                .header("Authorization", key)
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("[Discord Bots] Report failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    logger.info("[Discord Bots] Report sent.");
                } else {
                    logger.warn("[Discord Bots] Bad response: {} {}", response.code(), response.message());
                }
            }
        });
    }

    private void reportCarbonitex(String key) {
        bot.http.newCall(new Request.Builder()
                .post(new FormBody.Builder()
                            .add("key", key)
                            .add("servercount", str(bot.getShardUtil().getGuildCount()))
                            .build())
                .url(Endpoints.CARBONITEX.getUrl())
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("[Carbonitex] Report failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    logger.info("[Carbonitex] Report sent.");
                } else {
                    logger.warn("[Carbonitex] Bad response: {} {}", response.code(), response.message());
                }
            }
        });
    }
}
