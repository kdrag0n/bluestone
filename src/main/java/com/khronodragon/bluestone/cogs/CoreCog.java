package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreCog extends Cog {
    public CoreCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Core";
    }
    public String getDescription() {
        return "The core, essential cog to keep the bot running.";
    }

    @Command(name="ping", desc="Ping, pong!")
    public void cmdPing(Context ctx) {
        String msg = "Pong! WebSockets: " + ctx.jda.getPing() + "ms";
        long beforeTime = System.currentTimeMillis();

        ctx.send(msg).queue(message1 -> {
            message1.editMessage(msg + ", message: calculating...").queue(message2 -> {
                double msgPing = (System.currentTimeMillis() - beforeTime) / 2.0;
                message2.editMessage(msg + ", message: " + msgPing + "ms").queue();
            });
        });
    }

    @Command(name="rnum", desc="Get the current response number.")
    public void cmdRnum(Context ctx) {
        ctx.send("The current response number is " + ctx.responseNum + ".").queue();
    }

    @Command(name="help", desc="Because we all need help.")
    public void cmdHelp(Context ctx) {
        boolean sendPublic = false;
        if (ctx.invoker.startsWith("p")) {
            if (ctx.author.getIdLong() == bot.owner.getIdLong()) {
                sendPublic = true;
            }
        }

        List<EmbedBuilder> pages = new ArrayList<>();
        Map<String, List<Command>> cogAssign = new HashMap<>();
        Map<String, List<String>> fields = new HashMap<>();
        int chars = 0;

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setTitle("Bot Help");
    }

    @Command(name="extest", desc="For testing exception handling.")
    public void cmdEtest(Context ctx) throws Exception {
        throw new Exception("Testing exception handling in commands");
    }
}