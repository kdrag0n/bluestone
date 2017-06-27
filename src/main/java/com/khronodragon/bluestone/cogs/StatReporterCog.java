package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.logging.Level;

public class StatReporterCog extends Cog {
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

    @EventHandler(event = ReadyEvent.class)
    public void onReady(ReadyEvent event) {
        report();
    }

    @EventHandler(event = GuildJoinEvent.class)
    public void onGuildJoin(GuildJoinEvent event) {
        report();
    }

    @EventHandler(event = GuildLeaveEvent.class)
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

        Unirest.post(Endpoints.DISCORD_BOTS.format(bot.getJda().getSelfUser().getId()))
                .header("Authorization", key)
                .header("Content-Type", "application/json")
                .body(json.toString())
                .asStringAsync(new Callback<String>() {
                    @Override
                    public void completed(HttpResponse<String> response) {
                        if (response.getStatus() == 200) {
                            logger.info("[Discord Bots] Report sent.");
                        } else {
                            logger.warn("[Discord Bots] Bad response: {} {}", response.getStatus(), response.getStatusText());
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        logger.error("[Discord Bots] Report failed", e);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("[Discord Bots] Request cancelled");
                    }
                });
    }

    private void reportCarbonitex(String key) {
        Unirest.post(Endpoints.CARBONITEX.getUrl())
                .field("key", key)
                .field("servercount", bot.getShardUtil().getGuildCount())
                .asBinaryAsync(new Callback<InputStream>() {
                    @Override
                    public void completed(HttpResponse<InputStream> response) {
                        if (response.getStatus() == 200) {
                            logger.info("[Carbonitex] Report sent.");
                        } else {
                            logger.warn("[Carbonitex] Bad response: {} {}", response.getStatus(), response.getStatusText());
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        logger.error("[Carbonitex] Report failed", e);
                    }

                    @Override
                    public void cancelled() {
                        logger.error("[Carbonitex] Request cancelled");
                    }
                });
    }
}
