package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import okhttp3.Request;
import org.json.JSONArray;

/**
 * Wikipedia cog
 * @author Beefywhale
 */
public class WikiCog extends Cog {
    public WikiCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Wikipedia";
    }

    public String getDescription() {
        return "Wikipedia, the online encyclopedia.";
    }

    @Command(name = "wiki", desc = "Get a Wikipedia page.",
            usage = "[topic]", aliases = {"wikipedia"})
    public void cmdWiki(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("You need some search terms!");
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor("Wikipedia", "https://wikipedia.com/",
                "https://upload.wikimedia.org/wikipedia/en/thumb/8/80/Wikipedia-logo-v2.svg/1122px-Wikipedia-logo-v2.svg.png");

        Bot.http.newCall(new Request.Builder()
                .url("https://en.wikipedia.org/w/api.php?action=opensearch&search=" + ctx.rawArgs)
                .build()).enqueue(Bot.callback(response -> {

            if (!response.isSuccessful()) {
                emb.setDescription("⚠ Failed to get results from Wikipedia.");
                ctx.send(emb.build()).queue();
                return;
            }

            JSONArray json = new JSONArray(response.body().string());
            if (json.getJSONArray(1).length() < 1) {
                emb.setDescription("No results.");
                ctx.send(emb.build()).queue();
                return;
            }

            emb.setTitle(json.getJSONArray(1).getString(0))
                    .setDescription(json.getJSONArray(2).getString(0))
                    .addField("Link", json.getJSONArray(3).getString(0), false);
            ctx.send(emb.build()).queue();

            response.body().close();
        }, e -> {}));
    }

}
