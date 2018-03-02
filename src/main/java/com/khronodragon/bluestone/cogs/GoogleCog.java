package com.khronodragon.bluestone.cogs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.khronodragon.bluestone.util.Strings.format;

public class GoogleCog extends Cog {
    private static final Logger logger = LogManager.getLogger(GoogleCog.class);
    private static final String API_URL_BASE = "https://www.googleapis.com/customsearch/v1?key={0}&cx=011887893391472424519:xf_tuvgfrgk&q={1}";
    private static final MessageEmbed FAILED_EMBED = new EmbedBuilder()
            .setColor(randomColor())
            .setTitle("Google Search")
            .setAuthor("Google", "https://google.com/",
                    "https://raw.githubusercontent.com/Armored-Dragon/goldmine/master/assets/icon-google.png")
            .setDescription("⚠ Failed to get results from Google.")
            .build();
    private final LoadingCache<String, MessageEmbed> cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(new CacheLoader<String, MessageEmbed>() {
                @Override
                public MessageEmbed load(@Nonnull String key) throws IOException {
                    JSONObject resp = new JSONObject(Bot.http.newCall(new Request.Builder()
                            .get()
                            .url(key)
                            .build()).execute().body().string());
                    EmbedBuilder emb = new EmbedBuilder()
                            .setColor(randomColor())
                            .setTitle("Google Search")
                            .setAuthor("Google", "https://google.com/",
                                    "https://raw.githubusercontent.com/Armored-Dragon/goldmine/master/assets/icon-google.png");

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
                            } catch (JSONException | IllegalArgumentException ignored) {}
                        } else {
                            emb.setDescription("No results.");
                        }
                    } else if (resp.has("error")) {
                        logger.error("Google returned an error: {}", resp.getJSONObject("error"));
                        emb.setDescription("⚠ An error occurred, probably because I've searched too many times today.");
                    } else if (resp.has("searchInformation") && resp.getJSONObject("searchInformation").getInt("totalResults") < 1) {
                        emb.setDescription("No results.");
                    } else {
                        logger.info("Weird response from Google: {}", resp);
                        emb.setDescription("⚠ The response seems to have been invalid. Try again later?");
                    }

                    return emb.build();
                }
            });

    public GoogleCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Google";
    }

    public String getDescription() {
        return "A description.";
    }

    @Command(name = "google", desc = "We all need Google.", usage = "[search terms]", aliases = {"search"}, thread = true)
    public void cmdGoogle(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.fail("You need some search terms!");
            return;
        }

        final String query = ctx.args.join(' ');
        String key = bot.getKeys().optString("google");
        if (key == null) {
            ctx.fail("The bot doesn't have a Google API key set up!");
            return;
        }

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            bot.logger.error("System doesn't support UTF-8!", e);
            ctx.fail("The system this bot is running on doesn't support an essential encoding.");
            return;
        }
        ctx.channel.sendTyping().queue();

        try {
            ctx.send(cache.get(format(API_URL_BASE, key, encodedQuery +
                    ((ctx.channel instanceof TextChannel && ((TextChannel) ctx.channel).isNSFW()) ?
                            "&safe=off" : "&safe=medium")))).queue();
        } catch (ExecutionException|UncheckedExecutionException e) {
            logger.error("Failed to get results", e.getCause());
            ctx.send(FAILED_EMBED).queue();
        }
    }
}
