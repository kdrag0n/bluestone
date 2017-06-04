package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.entities.Message;
import org.apache.commons.lang3.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReplCog extends Cog {
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

    private String cleanupCode(String code) {
        return StringUtils.stripEnd(StringUtils.stripStart(StringUtils.replaceOnce(
                StringUtils.replaceOnce(
                        StringUtils.replaceOnce(code, "```scala", ""),
                        "```js", ""),
                "```javascript", ""), "`"), "`");
    }

    @Command(name="repl", desc="A multilingual REPL, in Discord!", thread=true)
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
                langs.add(String.format("%s %s (%s %s)", factory.getEngineName(), factory.getEngineVersion(),
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

        if (replSessions.contains(ctx.channelIdLong)) {
            ctx.send("Already running a REPL session in this channel. Exit it with `quit`.").queue();
            return;
        }
        replSessions.add(ctx.channelIdLong);

        engine.put("ctx", ctx);
        engine.put("context", ctx);
        engine.put("bot", ctx.bot);
        engine.put("last", null);
        engine.put("jda", ctx.jda);
        engine.put("message", ctx.message);
        engine.put("content", ctx.content);
        engine.put("author", ctx.author);
        engine.put("channel", ctx.channel);
        engine.put("guild", ctx.guild);
        engine.put("test", "Test right back at ya!");
        engine.put("msg", ctx.message);

        ctx.send("REPL started. Prefix is " + prefix).queue();
        while (true) {
            Message response = bot.waitForMessage(-1, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                    msg.getChannel().getIdLong() == ctx.channelIdLong &&
                    msg.getRawContent().startsWith(prefix));
            engine.put("message", response);
            engine.put("msg", response);

            String cleaned = cleanupCode(response.getRawContent());

            if (stringExists(cleaned, "quit", "exit", "System.exit()", "System.exit", "System.exit(0)")) {
                ctx.send("**Exiting...**").queue();
                replSessions.remove(ctx.channelIdLong);
                break;
            }

            Object result;
            try {
                result = engine.eval(cleaned);
            } catch (ScriptException e) {
                result = e;
            }

            if (result != null) {
                ctx.send("```py\n" + result.toString() + "```").queue();
            }
        }
    }
}

/*
String code = content.replaceFirst("```scala", "").replaceFirst("```js", "").replaceFirst("```javascript", "").stripPrefix("`").stripSuffix("`");
 */