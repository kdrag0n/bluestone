package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.sql.Pokemon;
import net.dv8tion.jda.core.EmbedBuilder;

import java.sql.SQLException;
import java.util.regex.Pattern;

import static com.khronodragon.bluestone.util.Strings.str;

public class PokemonCog extends Cog {
    private static final Pattern DIGIT3_PATTERN = Pattern.compile("^[0-9]{1,3}$");
    private static final Pattern PKNAME_PATTERN = Pattern.compile("^[a-zA-Z .]{1,16}$");
    private final Dao<Pokemon, Short> dao;

    public PokemonCog(Bot bot) {
        super(bot);

        dao = setupDao(Pokemon.class);
    }

    public String getName() {
        return "Pokemon";
    }

    @Override
    public String getCosmeticName() {
        return "Pokémon";
    }

    public String getDescription() {
        return "Gotta catch 'em all!";
    }

    @Command(name = "pokémon", desc = "Get info about a Pokémon!", thread = true,
            aliases = {"pokemon", "pokèmon", "pokedex", "pokédex"}, usage = "{name or id}")
    public void command(Context ctx) throws SQLException {
        byte mode; // 0 = id, 1 = name, 2 = random

        if (ctx.args.empty) {
            mode = 2;
        } else if (DIGIT3_PATTERN.matcher(ctx.rawArgs).matches()) {
            mode = 0;
        } else if (PKNAME_PATTERN.matcher(ctx.rawArgs).matches()) {
            mode = 1;
        } else {
            ctx.fail("Invalid Pokémon name/ID!");
            return;
        }

        ctx.channel.sendTyping().queue();

        Pokemon pokemon;
        switch (mode) {
            case 0:
                pokemon = dao.queryForId(Short.parseShort(ctx.rawArgs));
                break;
            case 1:
                pokemon = dao.queryBuilder()
                        .where()
                        .eq("name", ctx.rawArgs)
                        .queryForFirst();
                break;
            case 2:
                pokemon = dao.queryBuilder()
                        .orderByRaw("RAND()")
                        .limit(1L)
                        .queryForFirst();
                break;
            default:
                return;
        }

        final String imageUrl = "https://khronodragon.com/pokesprites/" + pokemon.id + ".png";
        String stats = "**ATK**: " +
                pokemon.atk +
                "\n**DEF**: " +
                pokemon.def;
        String specialStats =
                "**SP ATK**: " +
                pokemon.sp_atk +
                "\n**SP DEF**: " +
                pokemon.sp_def;

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor("#" + pokemon.id + " \u2022 " + pokemon.name, null, imageUrl)
                .setDescription(pokemon.description)
                .addField("HP", str(pokemon.hp), true)
                .addField("Stats", stats, true)
                .addField("Special Stats", specialStats, true)
                .addField("Speed", str(pokemon.speed), true)
                .addField("Types", pokemon.types, true)
                .setImage(imageUrl);

        ctx.send(emb.build()).queue();
    }
}
