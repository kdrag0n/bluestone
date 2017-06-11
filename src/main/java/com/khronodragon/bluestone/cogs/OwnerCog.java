package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;

public class OwnerCog extends Cog {
    private static final Logger logger = LogManager.getLogger(OwnerCog.class);
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
        ctx.send(":warning: Are you **sure** you want to stop the entire bot? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(7000, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                msg.getChannel().getIdLong() == ctx.channel.getIdLong() &&
                msg.getRawContent().equalsIgnoreCase("yes"));
        if (resp != null) {
            ctx.jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
            logger.info("Global shutdown requested.");
            if (ctx.jda.getShardInfo() == null) {
                ctx.jda.shutdown();
            } else {
                System.exit(0);
            }
        }
    }

    @Command(name = "stopshard", desc = "Stop the current shard.", perms = {"owner"}, aliases = {"restart"}, thread = true)
    public void cmdStopShard(Context ctx) {
        final Integer n = ctx.rawArgs.length() > 0 ? Integer.valueOf(ctx.rawArgs) : ctx.bot.getShardNum() - 1;
        ctx.send(":warning: Are you **sure** you want to stop (restart) shard " + n + "? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(7000, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                msg.getChannel().getIdLong() == ctx.channel.getIdLong() &&
                msg.getRawContent().equalsIgnoreCase("yes"));
        if (resp != null) {
            logger.info("Shard {} shutting down...", n);
            ctx.bot.getShardUtil().getShard(n).getJda().shutdown(false);
        }
    }

    @Command(name = "shardtree", desc = "Display a shard-guild tree.", perms = {"owner"})
    public void cmdShardTree(Context ctx) {
        List<String> items = new ArrayList<>();

        for (Bot shard: ctx.bot.getShardUtil().getShards()) {
            items.add("Shard " + shard.getJda().getShardInfo().getShardId() + ":");
            for (Guild guild: shard.getJda().getGuilds()) {
                items.add("    - " + guild.getName());
            }
        }

        ctx.send(format("```java\n{0}```", String.join("\n", items))).queue();
    }
}
