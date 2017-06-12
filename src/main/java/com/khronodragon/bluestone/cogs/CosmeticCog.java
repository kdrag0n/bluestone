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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CosmeticCog extends Cog {
    private static final Map<Character, String> alphabetToEmote = new HashMap<Character, String>() {{
        put(' ', "    ");
        put('#', ":hash:");
        put('!', ":exclamation:");
        put('?', ":question:");
        put('$', ":heavy_dollar_sign:");
        put('-', ":heavy_minus_sign:");
        put('.', ":small_blue_diamond:");
        put('~', ":wavy_dash:");
        put('0', ":zero:");
        put('1', ":one:");
        put('2', ":two:");
        put('3', ":three:");
        put('4', ":four:");
        put('5', ":five:");
        put('6', ":six:");
        put('7', ":seven:");
        put('8', ":eight:");
        put('9', ":nine:");
        put('^', ":arrow_up:");
        put('a', ":regional_indicator_a:");
        put('b', ":regional_indicator_b:");
        put('c', ":regional_indicator_c:");
        put('d', ":regional_indicator_d:");
        put('e', ":regional_indicator_e:");
        put('f', ":regional_indicator_f:");
        put('g', ":regional_indicator_g:");
        put('h', ":regional_indicator_h:");
        put('i', ":regional_indicator_i:");
        put('j', ":regional_indicator_j:");
        put('k', ":regional_indicator_k:");
        put('l', ":regional_indicator_l:");
        put('m', ":regional_indicator_m:");
        put('n', ":regional_indicator_n:");
        put('o', ":regional_indicator_o:");
        put('p', ":regional_indicator_p:");
        put('q', ":regional_indicator_q:");
        put('r', ":regional_indicator_r:");
        put('s', ":regional_indicator_s:");
        put('t', ":regional_indicator_t:");
        put('u', ":regional_indicator_u:");
        put('v', ":regional_indicator_v:");
        put('w', ":regional_indicator_w:");
        put('x', ":regional_indicator_x:");
        put('y', ":regional_indicator_y:");
        put('z', ":regional_indicator_z:");
    }};

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

    @Command(name = "emotisay", desc = "Show some text as cool block letters.", aliases = {"emotesay", "esay"}, usage = "[text]")
    public void cmdStyle(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: You need some text!").queue();
            return;
        }

        ctx.send(String.join("", ctx.rawArgs.chars().mapToObj(c -> {
            final Character character = (Character) (char) c;

            if (alphabetToEmote.containsKey(character)) {
                return alphabetToEmote.get(character);
            } else {
                return String.valueOf(character);
            }
        }).collect(Collectors.toList()))).queue();
    }

    @Command(name = "cookie", desc = "Cookie time!")
    public void cmdCookie(Context ctx) {
        ctx.send("\uD83C\uDF6A").queue();
    }

    @Command(name = "triggered", desc = "TRIGGERED")
    public void cmdTriggered(Context ctx) {
        ctx.send("***TRIGGERED***").queue();
    }

    @Command(name = "lenny", desc = "Le Lenny Face.")
    public void cmdLenny(Context ctx) {
        ctx.send("( ͡° ͜ʖ ͡°)").queue();
    }

    @Command(name = "tableflip", desc = "Flip that table!")
    public void cmdTableflip(Context ctx) {
        ctx.send("(╯°□°）╯︵ ┻━┻").queue();
    }

    @Command(name = "unflip", desc = "Flip that table back up!")
    public void cmdUnflip(Context ctx) {
        ctx.send("┬─┬\uFEFF ノ( ゜-゜ノ)").queue();
    }

    @Command(name = "hyflip", desc = "Is that table flipped or not? Oh wait, it's broken...")
    public void cmdHyflip(Context ctx) {
        ctx.send("(╯°□°）╯︵ ┻━─┬\uFEFF ノ( ゜-゜ノ)").queue();
    }

    @Command(name = "cat", desc = "Get a random cat!")
    public void cmdCat(Context ctx) {
        Unirest.get("http://random.cat/meow")
                .header("User-Agent", Bot.USER_AGENT)
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
