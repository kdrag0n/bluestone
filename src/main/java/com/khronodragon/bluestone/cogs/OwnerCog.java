package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.Cooldown;
import com.khronodragon.bluestone.enums.BucketType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.str;
import static java.text.MessageFormat.format;

public class OwnerCog extends Cog {
    private static final Logger logger = LogManager.getLogger(OwnerCog.class);
    private ScriptEngine evalEngine = new ScriptEngineManager().getEngineByName("groovy");
    private static final String EVAL_PRE_INJECT = "import net.dv8tion.jda.core.entities.*\n" +
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

    public OwnerCog(Bot bot) {
        super(bot);
        evalEngine.put("last", null);
        evalEngine.put("bot", bot);
        evalEngine.put("test", "Test right back at ya!");
    }

    public String getName() {
        return "Owner";
    }
    public String getDescription() {
        return "Commands for the bot owner.";
    }

    @Command(name = "shutdown", desc = "Shutdown the bot.", perms = {"owner"}, thread = true)
    public void cmdShutdown(Context ctx) {
        ctx.send(":warning: Are you **sure** you want to stop the entire bot? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(7000, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                msg.getChannel().getIdLong() == ctx.channel.getIdLong() &&
                msg.getRawContent().equalsIgnoreCase("yes"));
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

    @Command(name = "stopshard", desc = "Stop the current shard.", perms = {"owner"}, aliases = {"restart"}, thread = true)
    public void cmdStopShard(Context ctx) {
        final Integer n = ctx.rawArgs.length() > 0 ? Integer.valueOf(ctx.rawArgs) : ctx.bot.getShardNum() - 1;
        ctx.send(":warning: Are you **sure** you want to stop (restart) shard " + n + "? Type `yes` to continue.").complete();
        Message resp = bot.waitForMessage(7000, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                msg.getChannel().getIdLong() == ctx.channel.getIdLong() &&
                msg.getRawContent().equalsIgnoreCase("yes"));
        if (resp != null) {
            logger.info("Shard {} shutting down...", n);
            ctx.bot.getShardUtil().getShard(n).getJda().shutdown(false);
        }
    }

    @Command(name = "shardtree", desc = "Display a shard-guild tree.", perms = {"owner"})
    public void cmdShardTree(Context ctx) {
        List<String> items = new ArrayList<>();

        for (Bot shard: ctx.bot.getShardUtil().getShards()) {
            items.add("Shard " + (shard.getShardNum() - 1) + ':');
            for (Guild guild: shard.getJda().getGuilds()) {
                items.add("    - " + guild.getName());
            }
        }

        ctx.send(format("```java\n{0}```", String.join("\n", items))).queue();
    }

    @Cooldown(scope = BucketType.GLOBAL, delay = 10)
    @Command(name = "broadcast", desc = "Broadcast a message to all available guilds.",
            usage = "[message]", perms = {"owner"}, reportErrors = false)
    public void cmdBroadcast(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need a message to broadcast!").queue();
            return;
        }
        ctx.send("Starting broadcast...").queue();
        int errors = 0;

        for (Guild guild: ctx.jda.getGuilds()) {
            if (!guild.isAvailable()) {
                ctx.send(":warning: Guild **" + val(guild.getName()).or("[unknown]") +
                        "** (`" + val(guild.getIdLong()).or(0L) + "`) unavailable.").queue();
                errors++;
                continue;
            }
            String message = ctx.rawArgs.replace("%prefix%",
                    bot.getShardUtil().getPrefixStore().getPrefix(guild.getIdLong()));

            if (guild.getPublicChannel().canTalk())
                guild.getPublicChannel().sendMessage(message).queue();
            else {
                long publicId = guild.getPublicChannel().getIdLong();

                Optional<TextChannel> opt = guild.getTextChannels()
                        .stream()
                        .filter(c -> c.getIdLong() != publicId)
                        .filter(TextChannel::canTalk)
                        .findFirst();

                if (opt.isPresent())
                    opt.get().sendMessage(message).queue();
                else {
                    ctx.send(":warning: Guild **" + guild.getName() +
                            "** (`" + guild.getIdLong() + "`) muted us in all channels...").queue();
                    errors++;
                }
            }
        }

        ctx.send(":white_check_mark: Broadcast finished, with **" + errors + "** errored guilds.").queue();
    }

    @Command(name = "eval", desc = "Evaluate some Groovy code.", usage = "[code]",
            aliases = {"reval"}, perms = {"owner"}, thread = true, reportErrors = false)
    public void cmdEval(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need some code!").queue();
            return;
        }

        evalEngine.put("ctx", ctx);
        evalEngine.put("jda", ctx.jda);
        evalEngine.put("message", ctx.message);
        evalEngine.put("author", ctx.author);
        evalEngine.put("channel", ctx.channel);
        evalEngine.put("guild", ctx.guild);
        evalEngine.put("msg", ctx.message);

        Object result;
        try {
            result = evalEngine.eval(EVAL_PRE_INJECT + ReplCog.cleanupCode(ctx.rawArgs));
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
        else
            ctx.send("```java\n" + result.toString() + "```").queue();
    }

    @Command(name = "sendfile", desc = "Upload a local file here.", perms = {"owner"},
            usage = "[file path]", reportErrors = false)
    public void cmdSendfile(Context ctx) throws IOException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":thinking: I need a file path!").queue();
            return;
        }

        ctx.channel.sendFile(new File(ctx.rawArgs), new MessageBuilder().append(":e_mail: File incoming!")
                .build()).queue();
    }

    @Command(name = "command_calls", desc = "Get the top 25 command calls.", perms = {"owner"},
            hidden = true, aliases = {"ccalls"})
    public void cmdCmdCalls(Context ctx) {
        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor("Command Calls", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setDescription("Here are the top 25 command calls, sorted by amount.")
                .addField("Total", str(bot.commandCalls.values().stream()
                                        .mapToInt(AtomicInteger::get)
                                        .sum()), true);

        bot.commandCalls.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder(Comparator.comparingInt(AtomicInteger::get))))
                .limit(24)
                .forEach(entry -> {
                    emb.addField(entry.getKey(), str(entry.getValue().get()), true);
                });

        ctx.send(emb.build()).queue();
    }

    @Command(name = "setavatar", desc = "Change my avatar.", perms = {"owner"}, aliases = {"set_avatar"})
    public void cmdSetAvatar(Context ctx) throws IOException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":thinking: I need a file path!").queue();
            return;
        }

        ctx.jda.getSelfUser().getManager().setAvatar(Icon.from(new File(ctx.rawArgs))).queue();
        ctx.send(":thumbsup: Avatar changed.").queue();
    }
}
