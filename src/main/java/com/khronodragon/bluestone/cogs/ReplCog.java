package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.handlers.RMessageWaitListener;
import com.khronodragon.bluestone.util.StackUtil;
import com.khronodragon.bluestone.util.Switch;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.luaj.vm2.script.LuaScriptEngine;

import javax.script.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.khronodragon.bluestone.util.Strings.format;

@SuppressWarnings("SameParameterValue")
public class ReplCog extends Cog {
    private static final Logger logger = LogManager.getLogger(ReplCog.class);
    private static final String[] NASHORN_ARGS = {"--language=es6", "-scripting"};
    private static final Pattern JS_OBJECT_PATTERN = Pattern.compile("^\\[object [A-Z][a-z0-9]*]$");
    private static final Pattern CODE_TYPE_PATTERN = Pattern.compile("```(?:js|javascript|java|groovy|lua)\n?");
    static final String GROOVY_PRE_INJECT = "import net.dv8tion.jda.core.entities.*\n" +
            "import net.dv8tion.jda.core.*\n" +
            "import net.dv8tion.jda.core.entities.impl.*\n" +
            "import net.dv8tion.jda.core.audio.*\n" +
            "import net.dv8tion.jda.core.audit.*\n" +
            "import net.dv8tion.jda.core.managers.*\n" +
            "import net.dv8tion.jda.core.exceptions.*\n" +
            "import net.dv8tion.jda.core.events.*\n" +
            "import net.dv8tion.jda.core.utils.*\n" +
            "import com.khronodragon.bluestone.*\n" +
            "import org.apache.logging.log4j.*\n" +
            "import javax.script.*\n" +
            "import com.khronodragon.bluestone.cogs.*\n" +
            "import com.khronodragon.bluestone.errors.*\n" +
            "import org.json.*\n" +
            "import com.khronodragon.bluestone.sql.*\n" +
            "import com.khronodragon.bluestone.handlers.*\n" +
            "import com.khronodragon.bluestone.enums.*\n" +
            "import com.khronodragon.bluestone.util.*\n" +
            "import java.time.*\n" +
            "import java.math.*\n";

    private TLongSet replSessions = new TLongHashSet();

    public ReplCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "REPL";
    }
    public String getDescription() {
        return "A multilingual REPL, in Discord!";
    }

    public static String cleanUpCode(String code) {
        String stage1 = CODE_TYPE_PATTERN.matcher(code).replaceFirst("");
        return StringUtils.stripEnd(StringUtils.stripStart(stage1, "`"), "`");
    }

    @Perm.Owner
    @Command(name = "repl", desc = "A multilingual REPL, in Discord!\n\nFlags come before language in arguments.",
            usage = "[language] {flags}", thread=true)
    public void cmdRepl(Context ctx) throws ScriptException {
        if (ctx.args.length < 1) {
            ctx.fail("I need a valid language!");
            return;
        }

        String prefix = "`";
        boolean untrusted = false;
        String language = ctx.args.get(0);
        if (language.equalsIgnoreCase("untrusted")) {
            untrusted = true;

            if (ctx.args.length < 2) {
                ctx.fail("I need a valid language!");
                return;
            }

            language = ctx.args.get(1);
        }

        ScriptEngineManager man = new ScriptEngineManager();
        ScriptEngine engine;

        if (language.equalsIgnoreCase("list")) {
            List<ScriptEngineFactory> factories = man.getEngineFactories();
            List<String> langs = new ArrayList<>(factories.size());

            for (ScriptEngineFactory factory: factories) {
                langs.add(format("{0} {1} ({2} {3})", factory.getEngineName(), factory.getEngineVersion(),
                        factory.getLanguageName(), factory.getLanguageVersion()));
            }

            ctx.send("List of available languages:\n    \u2022 " + StringUtils.join(langs, "\n    \u2022 ")).queue();
            return;
        } else if (language.equalsIgnoreCase("nashorn") ||
                language.equalsIgnoreCase("js") ||
                language.equalsIgnoreCase("javascript")) {
            engine = new NashornScriptEngineFactory().getScriptEngine(NASHORN_ARGS);
        } else if (language.equalsIgnoreCase("kotlin") || language.equalsIgnoreCase("kt") ||
                language.equalsIgnoreCase("kts")) {
            engine = man.getEngineByExtension("kts"); // hacky workaround for Kotlin JSR-223 being ech
        } else {
            engine = man.getEngineByName(language.toLowerCase());

            if (engine == null) {
                ctx.fail("No such REPL language!");
                return;
            }
        }

        if (replSessions.contains(ctx.channel.getIdLong())) {
            ctx.send(Emotes.getFailure() + "REPL is already active in this channel.").queue();
            return;
        }

        replSessions.add(ctx.channel.getIdLong());
        OffsetDateTime startTime = OffsetDateTime.now();

        engine.put("ctx", ctx);
        engine.put("event", ctx.event);
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
        engine.put("startTime", startTime);
        engine.put("engine", engine);

        // ScriptEngine post-init
        try {
            new Switch<Class<? extends ScriptEngine>>(engine.getClass())
                    .byInstance()
                    .match(GroovyScriptEngineImpl.class, () -> engine.eval(
                            "def print = { Object... args -> ctx.send(Arrays.stream(args).map({ it.toString() }).collect(Collectors.joining(' '))).queue() }"
                    ))
                    .match(NashornScriptEngine.class, () -> {
                        // imports
                        StringBuilder importsObj = new StringBuilder("const imports=new JavaImporter(null");

                        for (String stmt : StringUtils.split(GROOVY_PRE_INJECT, '\n')) {
                            String pkg = StringUtils.split(stmt, ' ')[1];
                            pkg = pkg.substring(0, pkg.length() - 2);

                            importsObj.append(",Packages.")
                                    .append(pkg);
                        }

                        importsObj.append(')');

                        engine.eval(importsObj.toString() +
                                ";function loadShims(){load('https://raw.githubusercontent.com/es-shims/es5-shim/master/es5-shim.min.js');load('https://raw.githubusercontent.com/paulmillr/es6-shim/master/es6-shim.min.js');}");

                        // print
                        engine.eval("function print() { ctx.send.apply(ctx, arguments.join(' ')).queue() }; var console = {log: print};");
                    })
                    .match(LuaScriptEngine.class, () -> {
                        engine.eval("function print(...) ctx.send(string.join(' ', ...)).queue() end");
                        engine.put("__dirsep", System.getProperty("file.separator"));
                        engine.eval("debug = debug or {}; package = package or {}; package.config = package.config or (__dirsep .. '\\n;\\n?\\n!\\n-'); require 'assets.essentials'; require 'assets.cron'; require 'assets.middleclass'; require 'assets.stateful'; require 'assets.inspect'; require 'assets.repl_base'");
                    });
        } catch (Exception e) {
            ctx.send("⚠ Engine post-init failed.\n```java\n" + StackUtil.renderStackTrace(e) + "```").queue();
        }

        ctx.send(Emotes.getSuccess() + " **REPL started" + (untrusted ? " in untrusted mode" : "") +
                ".** Prefix is " + prefix).queue();
        while (true) {
            Message response;
            if (untrusted) {
                response = waitForRMessage(0, msg -> msg.getContentRaw().startsWith(prefix) &&
                        msg.getAuthor().getIdLong() != ctx.jda.getSelfUser().getIdLong(),
                        e -> e.getUser().getIdLong() == ctx.author.getIdLong() &&
                                !MiscUtil.getCreationTime(e.getMessageIdLong()).isBefore(startTime), ctx.channel.getIdLong());
            } else {
                response = bot.waitForMessage(0, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                        msg.getChannel().getIdLong() == ctx.channel.getIdLong() &&
                        msg.getContentRaw().startsWith(prefix));
            }

            if (untrusted && response.getAuthor().getIdLong() != ctx.author.getIdLong()) {
                Optional<MessageReaction> rr =
                        response.getReactions().stream().filter(r -> r.getReactionEmote().getName().equals("🛡")).findFirst();
                if (rr.isPresent()) {
                    MessageReaction mr = rr.get();
                    if (mr.getUsers().complete().stream().noneMatch(u -> u.getIdLong() == ctx.author.getIdLong())) {
                        continue;
                    }
                } else {
                    response.addReaction("🛡").queue();
                    continue;
                }
            }

            engine.put("message", response);
            engine.put("msg", response);
            String cleaned = cleanUpCode(response.getContentRaw());

            if (stringExists(cleaned, "quit", "exit", "System.exit()", "System.exit", "System.exit(0)",
                    "exit()", "stop", "stop()", "System.exit();", "stop;", "stop();", "System.exit(0);")) {
                replSessions.remove(ctx.channel.getIdLong());
                break;
            }

            Object result;
            try {
                if (engine instanceof GroovyScriptEngineImpl) {
                    result = engine.eval(GROOVY_PRE_INJECT + cleaned);
                } else if (engine instanceof LuaScriptEngine) {
                    int lastNidx = cleaned.lastIndexOf(10);
                    String code = lastNidx == -1 ? "" : cleaned.substring(0, lastNidx);
                    String lastLine = lastNidx == -1 ? cleaned : cleaned.substring(lastNidx + 1);

                    if (lastLine.equals("end")) {
                        code += "\nend";
                        lastLine = "return nil";
                    }

                    engine.put("__code", code);
                    engine.put("__last_line", lastLine);
                    engine.put("__orig", cleaned);

                    result = engine.eval("__repl_step()");
                } else {
                    result = engine.eval(cleaned);
                }
            } catch (ScriptException e) {
                result = e.getCause();

                if (result instanceof ScriptException) {
                    result = ((ScriptException) result).getCause();
                }
            } catch (Throwable e) {
                result = StackUtil.renderStackTrace(e);
            }

            if (result instanceof RestAction)
                result = ((RestAction) result).complete();
            else if (result instanceof Message)
                ctx.channel.sendMessage((Message) result).queue();
            else if (result instanceof EmbedBuilder)
                ctx.channel.sendMessage(((EmbedBuilder) result).build()).queue();
            else if (result instanceof MessageEmbed)
                ctx.channel.sendMessage((MessageEmbed) result).queue();

            engine.put("last", result);

            if (result != null) {
                try {
                    String strResult = result.toString();
                    if (engine instanceof NashornScriptEngine && JS_OBJECT_PATTERN.matcher(strResult).matches()) {
                        try {
                            strResult = (String) engine.eval("JSON.stringify(last)");
                        } catch (ScriptException e) {
                            strResult = StackUtil.renderStackTrace(e);
                        }
                    }

                    ctx.send("```java\n" + strResult + "```").queue();
                } catch (Exception e) {
                    logger.warn("Error sending result", e);
                    try {
                        ctx.send("```java\n" + StackUtil.renderStackTrace(e) + "```").queue();
                    } catch (Exception ex) {
                        logger.error("Error sending send error", ex);
                    }
                }
            } else {
                response.addReaction("✅").queue();
            }
        }

        ctx.success("REPL stopped.");
    }

    private Message waitForRMessage(long millis, Predicate<Message> check,
                                    Predicate<MessageReactionAddEvent> rCheck, long channelId) {
        AtomicReference<Message> lock = new AtomicReference<>();
        RMessageWaitListener listener = new RMessageWaitListener(lock, check, rCheck, channelId);
        bot.jda.addEventListener(listener);

        synchronized (lock) {
            try {
                lock.wait(millis);
            } catch (InterruptedException e) {
                bot.jda.removeEventListener(listener);
                return null;
            }
            return lock.get();
        }
    }
}