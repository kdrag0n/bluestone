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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.text.MessageFormat.format;

public class GoogleCog extends Cog {
    private static final Logger logger = LogManager.getLogger(GoogleCog.class);
    protected static final String API_URL_BASE = "https://www.googleapis.com/customsearch/v1?key={0}&cx=011887893391472424519:xf_tuvgfrgk&safe=off&q={1}";

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
        String key = bot.getKeys().get("google").getAsString();
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
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {
                        JSONObject resp = response.getBody().getObject();

                        if (resp.has("items")) {
                            JSONArray items = resp.getJSONArray("items");

                            if (items.length() > 0) {
                                JSONObject result = items.getJSONObject(0);

                                emb.setTitle(result.getString("title"))
                                        .setDescription(result.getString("snippet"))
                                        .addField("Link", result.getString("link"), false);

                                try {
                                    JSONObject meta = result.getJSONObject("pagemap").getJSONArray("metatags").getJSONObject(0);

                                    if (meta.has("twitter:image")) {
                                        if (meta.has("twitter:card")) {
                                            if (meta.getString("twitter:card").equals("summary_large_image")) {
                                                emb.setImage(meta.getString("twitter:image"));
                                            } else {
                                                emb.setThumbnail(meta.getString("twitter:image"));
                                            }
                                        } else {
                                            emb.setThumbnail(meta.getString("twitter:image"));
                                        }
                                    } else if (meta.has("og:image")) {
                                        emb.setThumbnail(meta.getString("og:image"));
                                    }
                                } catch (JSONException ignored) {}
                            } else {
                                emb.setDescription("No results.");
                            }
                        } else if (resp.has("error")) {
                            logger.error("Google returned an error: {}", resp.getJSONObject("error"));
                            emb.setDescription(":warning: An error occurred, probably because I've searched too many times today.");
                        } else if (resp.has("searchInformation") && resp.getJSONObject("searchInformation").getInt("totalResults") < 1) {
                            emb.setDescription("No results.");
                        } else {
                            logger.info("Weird response from Google: {}", resp);
                            emb.setDescription(":warning: The response seems to have been invalid. Try again later?");
                        }

                        ctx.send(emb.build()).queue();
                    }

                    @Override
                    public void failed(UnirestException e) {
                        bot.logger.error("Failed to get results", e);
                        emb.setDescription("⚠ Failed to get results from Google.");
                        ctx.send(emb.build()).queue();
                    }

                    @Override
                    public void cancelled() {
                        ctx.send(":x: The search was cancelled for some reason.").queue();
                    }
                });
    }
}
