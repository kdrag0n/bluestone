package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.text.MessageFormat.format;

public class ReplCog extends Cog {
    private static final Logger logger = LogManager.getLogger(ReplCog.class);
    private static final String GROOVY_PRE_INJECT = "import net.dv8tion.jda.core.entities.*\n" +
            "import net.dv8tion.jda.core.*\n" +
            "import com.khronodragon.bluestone.*\n" +
            "import org.apache.logging.log4j.*\n" +
            "import javax.script.*\n" +
            "import com.khronodragon.bluestone.cogs.*\n" +
            "import com.khronodragon.bluestone.errors.*\n" +
            "import org.json.*\n" +
            "import com.khronodragon.bluestone.sql.*\n" +
            "import com.khronodragon.bluestone.handlers.*\n" +
            "import com.khronodragon.bluestone.enums.*\n";

    private Set<Long> replSessions = new HashSet<Long>();

    public ReplCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "REPL";
    }
    public String getDescription() {
        return "A multilingual REPL, in Discord!";
    }

    public static String cleanupCode(String code) {
        String stage1 = code.replaceFirst("```(?:js|javascript|py|python|java|groovy|scala|kotlin|kt|lua|ruby|rb)\n?", "");
        return StringUtils.stripEnd(StringUtils.stripStart(stage1, "`"), "`");
    }

    @Command(name = "repl", desc = "A multilingual REPL, in Discord!", perms = {"owner"}, usage = "[language] {flags}", thread=true)
    public void cmdRepl(Context ctx) {
        if (ctx.args.size() < 1) {
            ctx.send("You need to specify a language, like `scala` or `js`!").queue();
            return;
        }

        String prefix = "`";
        String language = ctx.args.get(0);
        ScriptEngineManager man = new ScriptEngineManager();

        if (language.equalsIgnoreCase("list")) {
            List<ScriptEngineFactory> factories = man.getEngineFactories();
            List<String> langs = new ArrayList<>();

            for (ScriptEngineFactory factory: factories) {
                langs.add(format("{0} {1} ({2} {3})", factory.getEngineName(), factory.getEngineVersion(),
                        factory.getLanguageName(), factory.getLanguageVersion()));
            }

            ctx.send("List of available languages:\n    \u2022 " + StringUtils.join(langs, "\n    \u2022 ")).queue();
            return;
        }

        ScriptEngine engine = man.getEngineByName(language.toLowerCase());
        if (engine == null) {
            ctx.send(":x: Invalid REPL language!").queue();
            return;
        }

        if (replSessions.contains(ctx.channel.getIdLong())) {
            ctx.send("Already running a REPL session in this channel. Exit it with `quit`.").queue();
            return;
        }
        replSessions.add(ctx.channel.getIdLong());

        engine.put("ctx", ctx);
        engine.put("context", ctx);
        engine.put("bot", ctx.bot);
        engine.put("last", null);
        engine.put("jda", ctx.jda);
        engine.put("message", ctx.message);
        engine.put("author", ctx.author);
        engine.put("channel", ctx.channel);
        engine.put("guild", ctx.guild);
        engine.put("test", "Test right back at ya!");
        engine.put("msg", ctx.message);

        ctx.send("REPL started. Prefix is " + prefix).queue();
        while (true) {
            Message response = bot.waitForMessage(0, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                    msg.getChannel().getIdLong() == ctx.channel.getIdLong() &&
                    msg.getRawContent().startsWith(prefix));
            engine.put("message", response);
            engine.put("msg", response);

            String cleaned = cleanupCode(response.getRawContent());

            if (stringExists(cleaned, "quit", "exit", "System.exit()", "System.exit", "System.exit(0)", "exit()")) {
                ctx.send("**Exiting...**").queue();
                replSessions.remove(ctx.channel.getIdLong());
                break;
            }

            Object result;
            try {
                if (language.equals("groovy"))
                    result = GROOVY_PRE_INJECT + engine.eval(cleaned);
                else
                    result = engine.eval(cleaned);
            } catch (ScriptException e) {
                result = e.getCause();
                if (result instanceof ScriptException) {
                    result = ((ScriptException) result).getCause();
                }
            } catch (Throwable e) {
                logger.warn("Error executing code in REPL", e);
                result = bot.renderStackTrace(e);
            }
            if (result instanceof RestAction)
                result = ((RestAction) result).complete();
            engine.put("last", result);

            if (result != null) {
                try {
                    ctx.send("```java\n" + result.toString() + "```").queue();
                } catch (Exception e) {
                    logger.warn("Error sending message in REPL", e);
                    try {
                        ctx.send("```java\n" + bot.renderStackTrace(e) + "```").queue();
                    } catch (Exception ex) {
                        logger.error("Error reporting send error in REPL", ex);
                    }
                }
            } else {
                response.addReaction("✅").queue();
            }
        }
    }
}