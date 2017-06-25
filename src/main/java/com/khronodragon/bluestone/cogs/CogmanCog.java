package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.errors.PassException;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.util.Optional;
import java.util.Set;

public class CogmanCog extends Cog {
    private static final Logger logger = LogManager.getLogger(CogmanCog.class);
    private static final String DEFAULT_COGS_PATH = "com.khronodragon.bluestone.cogs";
    private final Reflections cogsReflector = new Reflections(DEFAULT_COGS_PATH);
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
    public void mainCmd(Context ctx) throws ReflectiveOperationException {
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

    private String getClassPath(Context ctx) {
        if (ctx.args.size() < 2) {
            ctx.send(":warning: I need a cog name or class path!").queue();
            throw new PassException();
        }
        String arg = ctx.args.get(1);

        if (!arg.matches("^(?:[a-z\\-_]+\\.)*[a-zA-Z0-9]+$")) {
            ctx.send(":warning: Invalid cog name or class path!").queue();
            throw new PassException();
        }

        String path;
        if (arg.indexOf('.') == -1)
            path = DEFAULT_COGS_PATH + '.' + arg;
        else
            path = arg;

        try {
            Class.forName(path);
        } catch (ClassNotFoundException e) {
            ctx.send(":warning: That class doesn't exist!").queue();
            throw new PassException();
        }

        return path;
    }

    private Cog ensureLoaded(Context ctx, String path) {
        Optional<Cog> opt = bot.cogs.values().stream()
                .filter(c -> c.getClass().getName().equals(path))
                .findFirst();
        if (opt.isPresent()) {
            return opt.get();
        } else {
            ctx.send(":warning: That cog isn't loaded!");
            throw new PassException();
        }
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

    private void cmdReload(Context ctx) throws ReflectiveOperationException {
        cmdUnload(ctx);
        cmdLoad(ctx);
    }

    private void cmdLoad(Context ctx) throws ReflectiveOperationException {
        String path = getClassPath(ctx);
        Class clazz = Class.forName(path);

        Cog cog = (Cog) clazz.getConstructor(Bot.class).newInstance(bot);
        bot.registerCog(cog);
        cog.load();

        ctx.send(":white_check_mark: Cog `" + clazz.getName() + "` loaded.").queue();
    }

    private void cmdUnload(Context ctx) {
        String path = getClassPath(ctx);
        Cog cog = ensureLoaded(ctx, path);

        bot.unregisterCog(cog);
        ctx.send(":white_check_mark: Cog `" + cog.getClass().getName() + "` unloaded.").queue();
    }

    private void cmdEnable(Context ctx) {
        String path = getClassPath(ctx);

        ctx.send(":x: This feature hasn't been implemented yet.").queue();
    }

    private void cmdDisable(Context ctx) {
        String path = getClassPath(ctx);

        ctx.send(":x: This feature hasn't been implemented yet.").queue();
    }

    private void cmdInfo(Context ctx) {
        String path = getClassPath(ctx);
        Cog cog = ensureLoaded(ctx, path);

        EmbedBuilder emb = newEmbedWithAuthor(ctx)
                .setColor(randomColor())
                .setTitle(cog.getCosmeticName() + " (" + cog.getName() + ')')
                .setDescription(cog.getDescription());

        ctx.send(emb.build()).queue();
    }
}
