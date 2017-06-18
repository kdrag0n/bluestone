package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.EventedCog;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.MessageFormat;

public class StatReporterCog extends Cog implements EventedCog {
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

    public void onReady(ReadyEvent event) {
        report();
    }

    public void onGuildJoin(GuildJoinEvent event) {
        report();
    }

    public void onGuildLeave(GuildLeaveEvent event) {
        report();
    }

    public void report() {
        String dbotsKey = bot.getKeys().optString("discord_bots");
        String carbonitexKey = bot.getKeys().optString("carbonitex");

        if (dbotsKey != null)
            reportDiscordBots(dbotsKey);

        if (carbonitexKey != null)
            reportCarbonitex(carbonitexKey);
    }

    protected void reportDiscordBots(String key) {
        JSONObject data = new JSONObject();
        data.put("guild_count", bot.getShardUtil().getGuildCount());

        Unirest.post(Endpoints.DISCORD_BOTS.format(bot.getJda().getSelfUser().getId()))
                .header("Authorization", key)
                .header("Content-Type", "application/json")
                .body(data)
                .asBinaryAsync(new Callback<InputStream>() {
                    @Override
                    public void completed(HttpResponse<InputStream> response) {
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

    protected void reportCarbonitex(String key) {
        JSONObject data = new JSONObject();
        data.put("key", key);
        data.put("servercount", bot.getShardUtil().getGuildCount());

        Unirest.post(Endpoints.CARBONITEX.getUrl())
                .body(data)
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
