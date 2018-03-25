package com.khronodragon.bluestone.cogs;

import com.censhare.db.iindex.IDSetTrie;
import com.j256.ormlite.dao.Dao;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import net.dv8tion.jda.core.EmbedBuilder;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Wikipedia Cog.
 * @author  Beefywhale
 */

public class WikiCog extends Cog {
    public WikiCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Wiki";
    }

    public String getDescription() {
        return "Get a wikipedia page.";
    }

    @Command(name = "wiki",
            desc = "Get a specific wikipedia page.",
            usage = "[topic]", thread = true, aliases = {"wikipedia"})
    public void wikiAfk(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("You need some search terms!");
            return;
        }
        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor("Wikipedia", "https://wikipedia.com/",
                "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/1122px-Wikipedia-logo-v2.svg.png");

        Bot.http.newCall(new Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?action=opensearch&search="+ctx.rawArgs)
                .build()).enqueue(Bot.callback(response -> {
            if (!response.isSuccessful()) {
                emb.setDescription("⚠ Failed to get results from Wikipedia.");
                ctx.send(emb.build()).queue();
                return;
            }

            JSONArray json = new JSONArray(response.body().string());
            emb.setDescription(json.getJSONArray(1).get(0).toString());
            emb.addField("Link", json.getJSONArray(3).get(0).toString(), false);
            ctx.send(emb.build()).queue();

            response.body().close();
        }, e -> {}));
    }

}
