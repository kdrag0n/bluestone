package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

public class CogmanCog extends Cog {
    public CogmanCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Cogman";
    }

    public String getDescription() {
        return "Manage all the cogs.";
    }

    @Command(name = "cog", desc = "Manage all the cogs.", perms = {"owner"}, aliases = {"cogs"})
    public void cmdCog(Context ctx) {
        // list, reload, load, unload, enable, disable, info
        ctx.send("Not implemented yet!").queue(); // TODO: implement
    }
}
