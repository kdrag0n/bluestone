package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashSet;
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

    @Command(name="repl", desc="A multilingual REPL, in Discord!")
    public void cmdRepl(Context ctx) {
        if (ctx.args.size() < 1) {
            ctx.send("You need to specify a language, like `scala` or `js`!").queue();
            return;
        }

        String prefix = "`";
        String language = ctx.args.get(0);
        print(language);

        if (replSessions.contains(ctx.channelIdLong)) {
            ctx.send("Already running a REPL session in this channel. Exit it with `quit`.").queue();
            return;
        }
        replSessions.add(ctx.channelIdLong);

        ScriptEngine engine = new ScriptEngineManager().getEngineByName(language);
        if (engine == null) {
            ctx.send(":x: Invalid REPL language!").queue();
            return;
        }

        engine.put("jda", ctx.jda);
        engine.put("message", ctx.message);
        engine.put("content", ctx.content);
        engine.put("author", ctx.author);
        engine.put("channel", ctx.channel);
        engine.put("guild", ctx.guild);
        engine.put("test", "Test right back at ya!");
        engine.put("msg", ctx.message);

        ctx.send("REPL started. `exit()` or `quit` to exit. Prefix is " + prefix).queue();
        while (true) {
            break;
        }
        ctx.send("not done yet").queue();
    }
}

/*
else if (replSessions.contains(channel.getIdLong)) {
            long ownerId = 160567046642335746;
            if (author.getId() == ownerId) {
                if (content.startsWith("`")) {
                    String code = content.replaceFirst("```scala", "").replaceFirst("```js", "").replaceFirst("```javascript", "").stripPrefix("`").stripSuffix("`");
                }
            }
        }
 */