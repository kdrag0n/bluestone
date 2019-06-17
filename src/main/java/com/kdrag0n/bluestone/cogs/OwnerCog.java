package com.kdrag0n.bluestone.cogs;

import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.annotations.Cooldown;
import com.kdrag0n.bluestone.enums.BucketType;
import com.kdrag0n.bluestone.util.StackUtil;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
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

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;

public class OwnerCog extends Cog {
    private static final Logger logger = LoggerFactory.getLogger(OwnerCog.class);
    private ScriptEngine evalEngine = new NashornScriptEngineFactory().getScriptEngine(ReplCog.NASHORN_ARGS);
    private final String token;

    public OwnerCog(Bot bot) {
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
    @Command(name = "shutdown", desc = "Shutdown the bot.", thread = true)
    public void cmdShutdown(Context ctx) {
        ctx.send(Emotes.getFailure() + " Are you **sure** you want to stop the entire bot? Type `yes` to continue.")
                .complete();
        Message resp = bot.waitForMessage(7000,
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
            "restart" }, thread = true, usage = "{shard}")
    public void cmdStopShard(Context ctx) {
        final Integer n = ctx.args.empty ? ctx.bot.getShardNum() - 1 : Integer.valueOf(ctx.rawArgs);
        ctx.send(Emotes.getFailure() + " Are you **sure** you want to stop (restart) shard " + n
                + "? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(7000,
                msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong()
                        && msg.getChannel().getIdLong() == ctx.channel.getIdLong()
                        && msg.getContentRaw().equalsIgnoreCase("yes"));
        if (resp != null) {
            logger.info("Shard {} shutting down...", n);
            ctx.bot.shardUtil.getShard(n).jda.shutdown();
        }
    }

    @Perm.Owner
    @Command(name = "shardinfo", desc = "Display global shard information.")
    public void cmdShardTree(Context ctx) {
        MessageBuilder result = new MessageBuilder().append("```css\n");

        for (Bot shard : ctx.bot.shardUtil.getShards()) {
            result.append('[').append(bot.getShardNum() == shard.getShardNum() ? '*' : ' ').append("] Shard ")
                    .append(shard.getShardNum() - 1).append(" | [").append(shard.jda.getStatus().name())
                    .append("] | Guilds: ").append(shard.jda.getGuilds().size()).append(" | Users: ")
                    .append(shard.jda.getUsers().size());

            MusicCog musicCog = (MusicCog) shard.cogs.get("Music");
            if (musicCog != null)
                result.append(" | MStreams: ").append(musicCog.getActiveStreamCount()).append(" | MTracks: ")
                        .append(musicCog.getTracksLoaded());

            result.append(" | WSPing: ").append(shard.jda.getPing()).append('\n');
        }
        result.append("\nTotal: ").append(bot.getShardTotal()).append(" shard(s)```");

        if (result.length() > 2000) {
            for (int i = 0; i < result.length(); i += 1999) {
                result.getStringBuilder().insert(i - 3, "``````css\n");
            }
        }

        for (Message msg : result.buildAll(MessageBuilder.SplitPolicy.ANYWHERE)) {
            ctx.send(msg).queue();
        }
    }

    @Perm.Owner
    @Cooldown(scope = BucketType.GLOBAL, delay = 10)
    @Command(name = "broadcast", desc = "Broadcast a message to all available guilds.", usage = "[message]")
    public void cmdBroadcast(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need a message to broadcast!");
            return;
        }

        String ss = "";
        if (ctx.jda.getShardInfo() != null) {
            ss = ctx.jda.getShardInfo().getShardString() + ' ';

            if (!ctx.flag) {
                ctx.flag = true;

                Collection<Bot> shards = bot.shardUtil.getShards();
                shards.remove(ctx.bot);

                for (Bot b : shards) {
                    if (b.cogs.containsKey("Owner")) {
                        ctx.bot = b;
                        ctx.jda = b.jda;
                        ((OwnerCog) b.cogs.get("Owner")).cmdBroadcast(ctx);
                    }
                }
            }
        }
        ctx.jda = bot.jda;
        ctx.bot = bot;

        ctx.send(ss + "Starting broadcast...").queue();
        int errors = 0;

        for (Guild guild : ctx.jda.getGuilds()) {
            if (!guild.isAvailable()) {
                ctx.send(Emotes.getFailure() + " Guild **" + val(guild.getName()).or("[unknown]") + "** (`"
                        + val(guild.getIdLong()).or(0L) + "`) unavailable.").queue();
                errors++;
                continue;
            }
            String message = StringUtils.replace(ctx.rawArgs, "%prefix%", bot.prefixStore.getPrefix(guild.getIdLong()));
            TextChannel channel = defaultWritableChannel(guild.getSelfMember());

            if (channel != null && channel.canTalk())
                channel.sendMessage(message).queue();
            else {
                errors++;
            }
        }

        ctx.success("Broadcast finished, with **" + errors + "** guilds all muted.");
    }

    @Perm.Owner
    @Command(name = "eval", desc = "Evaluate code.", usage = "[code]", aliases = {
            "reval" }, thread = true)
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
            result = evalEngine.eval(ReplCog.cleanUpCode(ctx.rawArgs));
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

            if (ReplCog.JS_OBJECT_PATTERN.matcher(strResult).matches()) {
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

        bot.shardUtil.getShards().forEach(b -> b.jda.getPresence().setGame(Game.playing(ctx.rawArgs)));
        ctx.success("Game set.");
    }
}
