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
import org.apache.commons.lang3.StringUtils;

public class CosmeticCog extends Cog {
    public CosmeticCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Cosmetic";
    }

    public String getDescription() {
        return "Some nice cosmetic stuff for me to have.";
    }

    @Command(name = "reverse", desc = "Reverse some text.", usage = "[text]")
    public void cmdReverse(Context ctx) {
        ctx.send(":repeat: " + StringUtils.reverse(ctx.rawArgs)).queue();
    }

    @Command(name = "cat", desc = "Get a random cat!")
    public void cmdCat(Context ctx) {
        Unirest.get("http://random.cat/meow")
                .asJsonAsync(new Callback<JsonNode>() {
                    public void completed(HttpResponse<JsonNode> response) {
                        String imageUrl = response.getBody().getObject().getString("file");
                        if (imageUrl == null) {
                            ctx.send(":warning: Couldn't get a cat!").queue();
                        } else {
                            ctx.send(new EmbedBuilder()
                                    .setImage(imageUrl)
                                    .setColor(randomColor())
                                    .build()).queue();
                        }
                    }

                    public void failed(UnirestException e) {
                        ctx.send(":warning: Failed to get a cat!").queue();
                    }

                    public void cancelled() {
                        ctx.send(":x: The request was cancelled!").queue();
                    }
                });
    }
}
