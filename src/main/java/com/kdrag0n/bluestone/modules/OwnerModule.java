package com.kdrag0n.bluestone.modules;

import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.types.Perm;
import com.kdrag0n.bluestone.util.StackUtil;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

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

    private int parseShardArgument(Context ctx) {
        int shard;
        if (ctx.args.empty) {
            shard = ctx.jda.getShardInfo().getShardId();
        } else {
            if (ctx.args.get(0).equals("all"))
                shard = -1;
            else
                shard = Integer.valueOf(ctx.args.get(0));
        }

        return shard;
    }

    private boolean confirmShardOperation(Context ctx) {
        ctx.send(Emotes.getFailure() + " Are you **sure** you want to stop shard `" +
                (ctx.args.empty ? ctx.jda.getShardInfo().getShardId() : ctx.args.get(0)) +
                "`? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(ctx.jda, 10000,
                msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong()
                        && msg.getChannel().getIdLong() == ctx.channel.getIdLong()
                        && msg.getContentRaw().equalsIgnoreCase("yes"),
                ctx.channel.getIdLong());

        return resp != null;
    }

    @Perm.Owner
    @Command(name = "stopshard", desc = "Stop the current shard, specified shard ID, or `all` shards.",
            aliases = {"stopbot", "shutdown"}, usage = "{shard or 'all'}")
    public void cmdStopShard(Context ctx) {
        int shard = parseShardArgument(ctx);

        if (confirmShardOperation(ctx)) {
            if (shard == -1)
                ctx.bot.manager.shutdown();
            else
                ctx.bot.manager.shutdown(shard);
        }
    }

    @Perm.Owner
    @Command(name = "restartshard", desc = "(Re)start the current shard, specified shard, or `all` shards.",
            aliases = {"restart", "restartbot", "startshard", "start"}, usage = "{shard or 'all'}")
    public void cmdRestartShard(Context ctx) {
        int shard = parseShardArgument(ctx);

        if (confirmShardOperation(ctx)) {
            if (shard == -1)
                ctx.bot.manager.restart();
            else
                ctx.bot.manager.restart(shard);
        }
    }

    @Perm.Owner
    @Command(name = "shardinfo", desc = "Display global shard information.")
    public void cmdShardInfo(Context ctx) {
        MessageBuilder result = new MessageBuilder().append("```css\n");
        int ctxShardId = ctx.jda.getShardInfo().getShardId();

        for (JDA shard : ctx.bot.getSortedShards()) {
            int shardId = shard.getShardInfo().getShardId();

            result.append('[')
                    .append(shardId == ctxShardId ? '*' : ' ')
                    .append("] Shard ")
                    .append(shardId)
                    .append(" | [")
                    .append(shard.getStatus().name())
                    .append("] | Guilds: ")
                    .append(shard.getGuilds().size())
                    .append(" | Users: ")
                    .append(shard.getUsers().size())
                    .append(" | WSPing: ")
                    .append(shard.getGatewayPing())
                    .append('\n');
        }

        result.append("\nTotal: ")
                .append(bot.manager.getShardsTotal())
                .append(" shard(s) | Guilds: ")
                .append(bot.getGuildCount())
                .append(" | Users: ")
                .append(bot.getUserCount())
                .append(" | Average WSPing: ")
                .append((int) Math.round(bot.manager.getAverageGatewayPing()))
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
        if (ctx.message.getAttachments().isEmpty()) {
            ctx.fail("I need an image to set as my avatar!");
            return;
        }

        try (InputStream imgStream = ctx.message.getAttachments().get(0).retrieveInputStream().get()) {
            ctx.jda.getSelfUser().getManager().setAvatar(Icon.from(imgStream)).queue();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        ctx.success("Avatar changed.");
    }

    @Perm.Owner
    @Command(name = "setactivity", desc = "Set my presence activity.", aliases = { "set_activity", "setgame", "set_game" })
    public void cmdSetActivity(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need an activity to set!");
            return;
        }

        bot.manager.getShards().forEach(shard -> shard.getPresence().setActivity(Activity.playing(ctx.rawArgs)));
        ctx.success("Activity set.");
    }
}
