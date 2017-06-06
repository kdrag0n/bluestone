package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.ShardUtil;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.util.HashSet;

public class UtilityCog extends Cog {
    public UtilityCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Utility";
    }
    public String getDescription() {
        return "Essential utility commands, as well as playful ones.";
    }

    @Command(name = "info", desc = "Get some info about me.", aliases = {"about", "stats", "statistics"})
    public void cmdInfo(Context ctx) {
        Runtime runtime = Runtime.getRuntime();
        ShardUtil shardUtil = bot.getShardUtil();
        MessageEmbed emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/")
                .setColor(randomColor())
                .setDescription("Made by **Dragon5232#1841**")
                .addField("Guilds", Integer.toString(shardUtil.getGuildCount()), true)
                .addField("Uptime", bot.formatUptime(), true)
                .addField("Requests", Integer.toString(shardUtil.getRequestCount()), true)
                .addField("Threads", Integer.toString(Thread.activeCount()), true)
                .addField("Memory Used", bot.formatMemory(), true)
                .addField("Users", Integer.toString(shardUtil.getUserCount()), true)
                .addField("Channels", Integer.toString(shardUtil.getChannelCount()), true)
                .addField("Commands", Integer.toString(new HashSet<>(bot.commands.values()).size()), true)
                .build();

        ctx.send(emb).queue();
    }
}
