package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

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

    @Command(name="ping", desc="Ping, pong!", aliases={"alias_test1", "alias_test2"})
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
        ctx.send("**Bluestone by Dragon5232**\n" +
                "\n" +
                "Commands:\n" +
                "  \\u2022 help - Show this help.\n" +
                "  \\u2022 ping - Ping, pong!\n" +
                "  \\u2022 rnum - Get the current response number.\n" +
                "\n" +
                "That's it for now.\n" +
                "Remember that this is a huge work in progress!\n" +
                "**IM REDOING THIS SOON!**\n" +
                "**like as soon as the command stuff starts working**").queue();
    }
}