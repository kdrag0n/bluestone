package com.kdrag0n.bluestone.cogs;

import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.errors.PassException;
import com.kdrag0n.bluestone.util.Strings;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class CogmanCog extends Cog {
    private static final Logger logger = LogManager.getLogger(CogmanCog.class);
    private static final String DEFAULT_COGS_PATH = "com.kdrag0n.bluestone.cogs";
    private final Reflections cogsReflector = new Reflections(DEFAULT_COGS_PATH);
    private static final Pattern CLASS_PATH_PATTERN = Pattern.compile("^[a-z]+://?(?:[a-zA-Z0-9\\-_:]+/)+[a-zA-Z0-9\\-_.]+\\.jar/(?:[a-z0-9\\-_]+\\.)*[a-zA-Z0-9]+$");
    private static final Pattern CLASS_FILE_PATTERN = Pattern.compile("^/?(?:.+/)+[a-zA-Z0-9]+(?:\\.class)?$");
    private static final Pattern END_CLASS_PATTERN = Pattern.compile("\\.class$");
    private static final String NO_COMMAND = "🤔 **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `list {package path}` - list cogs available (default path `com.kdrag0n.bluestone.cogs`)\n" +
            "    \u2022 `reload [cog]` - reload a cog, if loaded\n" +
            "    \u2022 `load [cog]` - load a cog\n" +
            "    \u2022 `unload [cog]` - unload a cog, if loaded\n" +
            "    \u2022 `enable [cog]` - enable a cog (will be loaded at startup)\n" +
            "    \u2022 `disable [cog]` - disable a cog (will not be loaded at startup)\n" +
            "    \u2022 `info [cog]` - view information about a cog\n" +
            "\n" +
            "Cog classes can be given by full path (`CogmanCog`), " +
            "or simple name (assumed to be in `com.kdrag0n.bluestone.cogs`).";
    private static final String INVALID_LOAD = Emotes.getFailure() + " I need a valid class URI, path, or name!\n" +
            "\n" +
            "Examples:\n" +
            "```\n" +
            "../../extra_cogs/AwesomeCog\n" +
            "file:///home/user/bot/cogs/AwesomeCog\n" +
            "https://koolio.github.io/bot_cogs/set1.jar/io.iamkool.cogs.AwesomeCog\n" +
            "CosmeticCog\n" +
            "FunCog\n" +
            "```";

    public CogmanCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Cogman";
    }

    public String getDescription() {
        return "Manage all the cogs.";
    }

    @Perm.Owner
    @Command(name = "cogall", desc = "Manage all the cogs - run on ALL shards.", aliases = {"cogsall"},
            reportErrors = false)
    public void mainCmdAll(Context ctx) throws ReflectiveOperationException, MalformedURLException {
        for (Bot shard: bot.shardUtil.getShards()) {
            CogmanCog cog = (CogmanCog) shard.cogs.getOrDefault("Cogman", null);

            if (cog != null) {
                ctx.bot = shard;
                ctx.jda = shard.jda;
                cog.mainCmd(ctx);
            }
        }
    }

    @Perm.Owner
    @Command(name = "cog", desc = "Manage all the cogs.", aliases = {"cogs"}, reportErrors = false)
    public void mainCmd(Context ctx) throws ReflectiveOperationException, MalformedURLException {
        if (ctx.args.empty) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        switch (invoked) {
            case "list":
                cmdList(ctx);
                break;
            case "reload":
                cmdReload(ctx);
                break;
            case "load":
                cmdLoad(ctx);
                break;
            case "unload":
                cmdUnload(ctx);
                break;
            case "enable":
                cmdEnable(ctx);
                break;
            case "disable":
                cmdDisable(ctx);
                break;
            case "info":
                cmdInfo(ctx);
                break;
            default:
                ctx.send(NO_COMMAND).queue();
                break;
        }
    }

    private String getClassPath(Context ctx) {
        if (ctx.args.length < 2) {
            ctx.fail("I need a cog name or class path!");
            throw new PassException();
        }
        String arg = ctx.args.get(1);

        if (!Strings.isPackage(arg)) {
            ctx.fail("Invalid cog name or class path!");
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
            ctx.fail("That class doesn't exist!");
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
            ctx.fail("That cog isn't loaded!");
            throw new PassException();
        }
    }

    private void cmdList(Context ctx) {
        Reflections reflector = cogsReflector;

        if (ctx.args.length > 1 && Strings.isPackage(ctx.args.get(1)))
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

    private void cmdReload(Context ctx) throws ReflectiveOperationException, MalformedURLException {
        cmdUnload(ctx);
        cmdLoad(ctx);
    }

    private void cmdLoad(Context ctx) throws ReflectiveOperationException, MalformedURLException {
        if (ctx.args.length < 2) {
            ctx.send(INVALID_LOAD).queue();
            return;
        }
        String input = ctx.args.get(1);

        Class clazz;
        if (CLASS_PATH_PATTERN.matcher(input).matches()) {
            String[] split = StringUtils.split(input, '/');
            String classPath = split[split.length - 1];
            String uriPath = StringUtils.join(ArrayUtils.remove(split, split.length - 1), '/');

            /*
            URLClassLoader cl = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(cl, new URL(uriPath));
            */
            ClassLoader cl = new URLClassLoader(new URL[] {new URL(uriPath)});

            clazz = cl.loadClass(classPath);
        } else if (Strings.isPackage(input)) {
            if (input.indexOf('.') == -1)
                clazz = Class.forName(DEFAULT_COGS_PATH + '.' + input);
            else
                clazz = Class.forName(input);
        } else if (CLASS_FILE_PATTERN.matcher(input).matches()) {
            File file = new File(input);
            URL url = file.getParentFile().toURI().toURL();
            String name = END_CLASS_PATTERN.matcher(file.getName()).replaceFirst("");

            ClassLoader cl = new URLClassLoader(new URL[] {url}, this.getClass().getClassLoader().getParent());
            clazz = cl.loadClass(name);
        } else {
            ctx.send(INVALID_LOAD).queue();
            return;
        }
        logger.info("new class url {}", clazz.getProtectionDomain().getCodeSource().getLocation());

        if (bot.cogs.values().stream()
                .anyMatch(c -> c.getClass().equals(clazz))) {
            ctx.fail("Cog already loaded!");
            return;
        }

        Cog cog = (Cog) clazz.getConstructor(Bot.class).newInstance(bot);
        bot.registerCog(cog);
        cog.load();

        ctx.success("Cog `" + clazz.getName() + "` loaded.");
    }

    private void cmdUnload(Context ctx) {
        String path = getClassPath(ctx);
        Cog cog = ensureLoaded(ctx, path);

        bot.unregisterCog(cog);
        ctx.success("Cog `" + cog.getClass().getName() + "` unloaded.");
    }

    private void cmdEnable(Context ctx) {
        String path = getClassPath(ctx);

        ctx.fail("This feature hasn't been implemented yet.");
    }

    private void cmdDisable(Context ctx) {
        String path = getClassPath(ctx);

        ctx.fail("This feature hasn't been implemented yet.");
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
