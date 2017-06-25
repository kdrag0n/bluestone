package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.util.Set;

public class CogmanCog extends Cog {
    private static final Logger logger = LogManager.getLogger(CogmanCog.class);
    private final Reflections cogsReflector = new Reflections("com.khronodragon.bluestone.cogs");
    private static final String NO_COMMAND = ":thinking: **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `list {package path}` - list cogs available (default path `com.khronodragon.bluestone.cogs`)\n" +
            "    \u2022 `reload [cog]` - reload a cog, if loaded\n" +
            "    \u2022 `load [cog]` - load a cog\n" +
            "    \u2022 `unload [cog]` - unload a cog, if loaded\n" +
            "    \u2022 `enable [cog]` - enable a cog (will be loaded at startup)\n" +
            "    \u2022 `disable [cog]` - disable a cog (will not be loaded at startup)\n" +
            "    \u2022 `info [cog]` - view information about a cog\n" +
            "\n" +
            "Cog classes can be given by full path (`com.khronodragon.bluestone.cogs.CogmanCog`), " +
            "or simple name (assumed to be in `com.khronodragon.bluestone.cogs`).";

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
    public void mainCmd(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        if (invoked.equals("list"))
            cmdList(ctx);
        else if (invoked.equals("reload"))
            cmdReload(ctx);
        else if (invoked.equals("load"))
            cmdLoad(ctx);
        else if (invoked.equals("unload"))
            cmdUnload(ctx);
        else if (invoked.equals("enable"))
            cmdEnable(ctx);
        else if (invoked.equals("disable"))
            cmdDisable(ctx);
        else if (invoked.equals("info"))
            cmdInfo(ctx);
        else
            ctx.send(NO_COMMAND).queue();
    }

    private void cmdList(Context ctx) {
        Reflections reflector = cogsReflector;

        if (ctx.args.size() > 1 && ctx.args.get(1).matches("^(?:[a-z\\-_]+\\.)+[a-zA-Z0-9]+$"))
            reflector = new Reflections(ctx.args.get(0));

        Set<Class<? extends Cog>> classes = reflector.getSubTypesOf(Cog.class);
        StringBuilder result = new StringBuilder("```css\n");

        if (classes.size() < 1)
            result.append("Package contains no cogs! \uD83D\uDE22\n");

        for (Class<? extends Cog> clazz: classes) {
            result.append('[');

            if (bot.cogs.values().stream().map(Cog::getClass)
                    .anyMatch(c -> c.getName().equals(clazz.getName())))
                result.append('*');
            else
                result.append(' ');

            result.append("] ")
                    .append(clazz.getSimpleName())
                    .append('\n');
        }

        result.append("\nLegend:\n  [*] Loaded Cog\n  [ ] Unloaded Cog```");

        ctx.send(result.toString()).queue();
    }

    private void cmdReload(Context ctx) {

    }

    private void cmdLoad(Context ctx) {

    }

    private void cmdUnload(Context ctx) {

    }

    private void cmdEnable(Context ctx) {

    }

    private void cmdDisable(Context ctx) {

    }

    private void cmdInfo(Context ctx) {

    }
}
