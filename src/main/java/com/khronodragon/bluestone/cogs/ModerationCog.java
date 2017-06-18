package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Permissions;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.Permission;

public class ModerationCog extends Cog {
    public ModerationCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Moderation";
    }

    public String getDescription() {
        return "Some handy moderation tools.";
    }

    @Command(name = "purge", desc = "Purge messages from a channel.", guildOnly = true, perms = {"messageManage", "messageHistory"})
    public void cmdPurge(Context ctx) {
        if (bot.isSelfbot()) {
            ctx.send(":x: Discord doesn't allow selfbots to purge.").queue();
            return;
        }

        ctx.send("WIP").queue();
    }
}
