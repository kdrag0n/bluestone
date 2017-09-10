package com.khronodragon.bluestone.cogs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.pokemon.Description;
import com.khronodragon.bluestone.pokemon.Pokemon;
import com.khronodragon.bluestone.util.IntegerZeroTypeAdapter;
import net.dv8tion.jda.core.EmbedBuilder;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.khronodragon.bluestone.util.Strings.str;
import static java.text.MessageFormat.format;

public class PokemonCog extends Cog {
    private static final Logger logger = LogManager.getLogger(PokemonCog.class);
    private static final String BASE_URI = "https://pokeapi.co";
    private static final Pattern D3_PATTERN = Pattern.compile("^[0-9]{1,3}$");
    private static final Pattern PKNAME_PATTERN = Pattern.compile("^[a-zA-Z .]{1,16}$");
    private static final int ENTRY_COUNT = 709;
    private static final Gson pokeGson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Integer.class, new IntegerZeroTypeAdapter())
            .create();
    private final LoadingCache<String, String> descCache = CacheBuilder.newBuilder()
            .maximumSize(72)
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws IOException {
                    return new JSONObject(Bot.http.newCall(new Request.Builder()
                            .get()
                            .url(key)
                            .build()).execute().body().string()).getString("description");
                }
            });
    private final LoadingCache<String, Pokemon> pokeCache = CacheBuilder.newBuilder()
            .maximumSize(72)
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(new CacheLoader<String, Pokemon>() {
                @Override
                public Pokemon load(String key) throws IOException {
                    return pokeGson.fromJson(Bot.http.newCall(new Request.Builder()
                            .get()
                            .url(key)
                            .build()).execute().body().string(), Pokemon.class);
                }
            });

    public PokemonCog(Bot bot) {
        super(bot);
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

    @Command(name = "pokémon", desc = "Get the info on a Pokémon!", thread = true,
            aliases = {"pokemon", "pokèmon", "pokedex", "pokédex"}, usage = "{name or id}")
    public void command(Context ctx) {
        StringBuilder url = new StringBuilder("https://pokeapi.co/api/v1/pokemon/");

        if (ctx.rawArgs.length() < 1) {
            url.append(randint(1, ENTRY_COUNT));
        } else {
            if (D3_PATTERN.matcher(ctx.rawArgs).matches()) {
                int requested = Integer.parseInt(ctx.rawArgs);

                if (requested >= 1 && requested <= ENTRY_COUNT)
                    url.append(requested);
                else {
                    ctx.send(Emotes.getFailure() + " Invalid national ID!").queue();
                    return;
                }
            } else if (PKNAME_PATTERN.matcher(ctx.rawArgs).matches()) {
                String pokemonName = StringUtils.remove(ctx.rawArgs.toLowerCase()
                        .replace(' ', '-'), '.');

                url.append(pokemonName);
            } else {
                ctx.send(Emotes.getFailure() + " Invalid Pokemon name/national ID!").queue();
                return;
            }
        }
        url.append('/');
        ctx.channel.sendTyping().queue();

        Pokemon pokemon;
        try {
            pokemon = pokeCache.get(url.toString());
        } catch (ExecutionException container) {
            Throwable e = container.getCause();
            logger.warn("Error contacting PokeAPI", e);
            ctx.send(Emotes.getFailure() + " Failed to fetch Pokémon. `" + e.getMessage() + '`').queue();
            return;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            ctx.send(Emotes.getFailure() + " No such Pokémon!").queue();
            return;
        }

        String descUrl = BASE_URI + Arrays.stream(pokemon.getDescriptions())
                .sorted(Collections.reverseOrder(Comparator.comparingInt(d ->
                        Integer.parseInt(StringUtils.split(d.name, '_')[2]))))
                .findFirst()
                .orElse(new Description()).resourceUri;
        String desc;

        try {
            desc = descCache.get(descUrl);
        } catch (ExecutionException container) {
            Throwable e = container.getCause();
            logger.warn("Error contacting PokeAPI", e);
            ctx.send(Emotes.getFailure() + " Failed to fetch description. `" + e.getMessage() + '`').queue();
            return;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            ctx.send(Emotes.getFailure() + " Pokémon has no description!").queue();
            return;
        }

        String imageUrl = format("https://pokeapi.co/media/img/{0}.png", pokemon.getNationalId());
        String stats = new StringBuilder()
                .append("**HP**: ")
                .append(pokemon.getHp())
                .append('\n')
                .append("**ATK**: ")
                .append(pokemon.getAttack())
                .append('\n')
                .append("**DEF**: ")
                .append(pokemon.getDefense())
                .append('\n')
                .append("**SP ATK**: ")
                .append(pokemon.getSpecialAttack())
                .append('\n')
                .append("**SP DEF**: ")
                .append(pokemon.getSpecialDefense())
                .toString();
        String evoString = Arrays.stream(pokemon.getEvolutions())
                .map(e -> WordUtils.capitalizeFully(e.to.replace('-', ' ')))
                .distinct()
                .collect(Collectors.joining(", "));

        float height = pokemon.getHeight() / 10.0f;
        double heightInches = height / .3048 % 1 * 12;
        float weight = pokemon.getWeight() / 10.0f;

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor("#" + pokemon.getNationalId() + " - " +
                        WordUtils.capitalizeFully(pokemon.getName().replace('-', ' ')),
                        null, imageUrl)
                .setDescription(desc)
                .addField("Stats", stats, true)
                .addField("Height", format("{0,number,0.#} m ({1,number,0} ft {2,number,0.##} in)",
                        height, Math.floor(heightInches / 12), heightInches % 12), true)
                .addField("Weight", format("{0,number,0.#} kg ({1,number,0.##} lb)",
                        weight, weight * 2.2), true)
                .addField("Speed", str(pokemon.getSpeed()), true)
                .addField("Abilities", Arrays.stream(pokemon.getAbilities())
                        .map(a -> WordUtils.capitalizeFully(a.name.replace('-', ' ')))
                        .collect(Collectors.joining(", ")), true)
                .addField("Types", Arrays.stream(pokemon.getTypes())
                        .map(t -> WordUtils.capitalizeFully(t.name))
                        .collect(Collectors.joining(", ")), true)
                .addField("Experience", str(pokemon.getExp()), true)
                .addField("Happiness", str(pokemon.getHappiness()), true)
                .addField("Evolves Into", "".equals(evoString) ? "Nothing" : evoString, true)
                .addField("Species", pokemon.getSpecies().equals("") ?
                        "¯\\_(ツ)_/¯" : pokemon.getSpecies(), true)
                .setImage(imageUrl);

        ctx.send(emb.build()).queue();
    }
}
