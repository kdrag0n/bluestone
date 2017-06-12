package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.text.MessageFormat.format;

public class GoogleCog extends Cog {
    private static final Logger logger = LogManager.getLogger(GoogleCog.class);
    protected static final String API_URL_BASE = "https://www.googleapis.com/customsearch/v1?key={0}&cx011887893391472424519:xf_tuvgfrgk&safe=off&q={1}";
    public GoogleCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Google";
    }

    public String getDescription() {
        return "A description.";
    }

    @Command(name = "google", desc = "We all need Google.", usage = "[search terms]", aliases = {"search"})
    public void cmdGoogle(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: You need some search terms!").queue();
            return;
        }

        final String query = String.join(" ", ctx.args);
        String key = bot.getAuth().getAsJsonObject("keys").get("google").getAsString();
        if (key == null) {
            ctx.send(":x: The bot doesn't have a Google API key set up!").queue();
            return;
        }

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            bot.logger.error("System doesn't support UTF-8!", e);
            ctx.send(":x: The system this bot is running on doesn't support an essential encoding.").queue();
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setTitle("Google Search")
                .setAuthor("Google", "https://google.com/", "https://raw.githubusercontent.com/Armored-Dragon/goldmine/master/assets/icon-google.png");

        Unirest.get(format(API_URL_BASE, key, encodedQuery))
                .header("User-Agent", Bot.USER_AGENT)
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {

                    }

                    @Override
                    public void failed(UnirestException e) {
                        bot.logger.error("Failed to get results", e);
                        emb.setDescription("⚠ Failed to get results from Google.");
                        ctx.send(emb.build()).queue();
                    }

                    @Override
                    public void cancelled() {

                    }
                });
    }
}
