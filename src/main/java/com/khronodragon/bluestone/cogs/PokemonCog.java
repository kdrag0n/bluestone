package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

public class PokemonCog extends Cog {
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

    @Command(name = "pokémon", desc = "Get the info on a Pokémon!",
            aliases = {"pokemon", "pokèmon", "pokedex", "pokédex"}, usage = "{name or id}?")
    public void command(Context ctx) {

    }
}
