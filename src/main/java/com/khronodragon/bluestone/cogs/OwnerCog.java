package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Message;

public class OwnerCog extends Cog {
    public OwnerCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Owner";
    }
    public String getDescription() {
        return "Commands for the bot owner.";
    }

    @Command(name = "shutdown", desc = "Shutdown the bot.", perms = {"owner"}, thread = true)
    public void cmdShutdown(Context ctx) {
        ctx.send(":warning: Are you **sure** you want to stop the whole bot? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(7.0f, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                msg.getChannel().getIdLong() == ctx.channelIdLong &&
                msg.getRawContent().toLowerCase().startsWith("yes"));
        if (resp != null) {
            ctx.jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
            print("Global shutdown requested.");
            if (ctx.jda.getShardInfo() == null) {
                ctx.jda.shutdown();
            } else {
                System.exit(0);
            }
        }
    }

    @Command(name = "stopshard", desc = "Stop the current shard.", perms = {"owner"}, aliases = {"restart"}, thread = true)
    public void cmdStopShard(Context ctx) {
        ctx.send(":warning: Are you **sure** you want to stop (restart) the shard? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(7.0f, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                msg.getChannel().getIdLong() == ctx.channelIdLong &&
                msg.getRawContent().toLowerCase().startsWith("yes"));
        if (resp != null) {
            ctx.jda.shutdown();
        }
    }
}
