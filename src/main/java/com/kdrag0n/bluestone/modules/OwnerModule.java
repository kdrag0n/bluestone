package com.kdrag0n.bluestone.modules;

import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.annotations.Cooldown;
import com.kdrag0n.bluestone.types.BucketType;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.types.Perm;
import com.kdrag0n.bluestone.util.StackUtil;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;

public class OwnerModule extends Module {
    private static final Logger logger = LoggerFactory.getLogger(OwnerModule.class);
    private ScriptEngine evalEngine = new NashornScriptEngineFactory().getScriptEngine(ReplModule.NASHORN_ARGS);
    private final String token;

    public OwnerModule(Bot bot) {
        super(bot);

        token = bot.getConfig().getString("token");
        evalEngine.put("last", null);
        evalEngine.put("bot", bot);
        evalEngine.put("test", "Test right back at ya!");
    }

    public String getName() {
        return "Owner";
    }

    @Perm.Owner
    @Command(name = "shutdown", desc = "Shutdown the bot.")
    public void cmdShutdown(Context ctx) {
        ctx.send(Emotes.getFailure() + " Are you **sure** you want to stop the entire bot? Type `yes` to continue.")
                .complete();
        Message resp = bot.waitForMessage(ctx.jda, 7000,
                msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong()
                        && msg.getChannel().getIdLong() == ctx.channel.getIdLong()
                        && msg.getContentRaw().equalsIgnoreCase("yes"));
        if (resp != null) {
            ctx.jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
            logger.info("Global shutdown requested.");
            if (ctx.jda.getShardInfo() == null) {
                ctx.jda.shutdown();
            } else {
                System.exit(0);
            }
        }
    }

    @Perm.Owner
    @Command(name = "stopshard", desc = "Stop the current shard.", aliases = {
            "restart" }, usage = "{shard}")
    public void cmdStopShard(Context ctx) {
        final Integer n = ctx.args.empty ? ctx.getShardNum() - 1 : Integer.valueOf(ctx.rawArgs);
        ctx.send(Emotes.getFailure() + " Are you **sure** you want to stop (restart) shard " + n
                + "? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(ctx.jda, 7000,
                msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong()
                        && msg.getChannel().getIdLong() == ctx.channel.getIdLong()
                        && msg.getContentRaw().equalsIgnoreCase("yes"));
        if (resp != null) {
            logger.info("Shard {} shutting down...", n);
            ctx.bot.manager.getShardById(n).shutdown();
        }
    }

    @Perm.Owner
    @Command(name = "shardinfo", desc = "Display global shard information.")
    public void cmdShardInfo(Context ctx) {
        MessageBuilder result = new MessageBuilder().append("```css\n");

        for (JDA shard : ctx.bot.getSortedShards()) {
            result.append('[')
                    .append(ctx.getShardNum() == Bot.getShardNum(shard) ? '*' : ' ')
                    .append("] Shard ")
                    .append(Bot.getShardNum(shard) - 1)
                    .append(" | [")
                    .append(shard.getStatus().name())
                    .append("] | Guilds: ")
                    .append(shard.getGuilds().size())
                    .append(" | Users: ")
                    .append(shard.getUsers().size())
                    .append(" | WSPing: ")
                    .append(shard.getPing())
                    .append('\n');
        }

        result.append("\nTotal: ")
                .append(bot.manager.getShardsTotal())
                .append(" shard(s) | Guilds: ")
                .append(bot.getGuildCount())
                .append(" | Users: ")
                .append(bot.getUserCount())
                .append(" | Average WSPing: ")
                .append((int) Math.round(bot.manager.getAveragePing()))
                .append('\n');

        MusicModule musicModule = (MusicModule) bot.modules.get("Music");
        if (musicModule != null) {
            result.append("[Music] MStreams: ")
                    .append(musicModule.getActiveStreamCount())
                    .append(" | MTracks: ")
                    .append(musicModule.getTracksLoaded());
        }

        result.append("```");

        if (result.length() > 2000) {
            for (int i = 0; i < result.length(); i += 1999)
                result.getStringBuilder().insert(i - 3, "``````css\n");
        }

        for (Message msg : result.buildAll(MessageBuilder.SplitPolicy.ANYWHERE))
            ctx.send(msg).queue();
    }

    @Perm.Owner
    @Command(name = "eval", desc = "Evaluate code.", usage = "[code]")
    public void cmdEval(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need code!");
            return;
        }

        evalEngine.put("ctx", ctx);
        evalEngine.put("event", ctx.event);
        evalEngine.put("jda", ctx.jda);
        evalEngine.put("message", ctx.message);
        evalEngine.put("author", ctx.author);
        evalEngine.put("channel", ctx.channel);
        evalEngine.put("guild", ctx.guild);
        evalEngine.put("msg", ctx.message);

        Object result;
        try {
            result = evalEngine.eval(ReplModule.cleanUpCode(ctx.rawArgs));
        } catch (ScriptException e) {
            result = e.getCause();
            if (result instanceof ScriptException) {
                result = ((ScriptException) result).getCause();
            }
        }

        if (result instanceof RestAction)
            result = ((RestAction) result).complete();
        evalEngine.put("last", result);

        if (result == null)
            ctx.message.addReaction("✅").queue();
        else {
            String strResult = result.toString();

            if (ReplModule.JS_OBJECT_PATTERN.matcher(strResult).matches()) {
                try {
                    strResult = (String) evalEngine.eval("JSON.stringify(last)");
                } catch (ScriptException e) {
                    strResult = StackUtil.renderStackTrace(e);
                }
            }

            ctx.send("```js\n" + StringUtils.replace(strResult, token, "") + "```").queue();
        }
    }

    @Perm.Owner
    @Command(name = "setavatar", desc = "Change my avatar.", aliases = { "set_avatar" })
    public void cmdSetAvatar(Context ctx) throws IOException {
        if (ctx.args.empty) {
            ctx.fail("I need a file path!");
            return;
        }

        ctx.jda.getSelfUser().getManager().setAvatar(Icon.from(new File(ctx.rawArgs))).queue();
        ctx.success("Avatar changed.");
    }

    @Perm.Owner
    @Command(name = "setgame", desc = "Set my game.", aliases = { "set_game" })
    public void cmdSetGame(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need a game to set!");
            return;
        }

        bot.manager.getShards().forEach(shard -> shard.getPresence().setGame(Game.playing(ctx.rawArgs)));
        ctx.success("Game set.");
    }
}
