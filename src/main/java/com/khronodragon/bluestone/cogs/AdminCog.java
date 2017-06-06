package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

public class AdminCog extends Cog {
    public AdminCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Admin";
    }

    public String getDescription() {
        return "Everything admin and moderator!";
    }

    @Command(name = "ban", desc = "Ban someone!", perms = {"banMembers"})
    public void cmdBan(Context ctx) {
        ctx.send("Not implemented yet!").queue();
    }
}