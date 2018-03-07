package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.enums.MessageDestination;
import com.khronodragon.bluestone.util.Paginator;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CoreCog extends Cog {
    private static final Logger logger = LogManager.getLogger(CoreCog.class);
    private static final String JOIN_MESSAGE =
            "By adding this bot, you agree that the activity of all users in this server *may* be logged, depending on features used or enabled.\n" +
                    "Features that may log data: quotes, starboard, etc. (this is to comply with the Discord ToS.)\n\n" +
                    "**Enjoy this bot!**\n" +
                    "\n" +
                    "If you ever have questions, *please* read the **FAQ** first: <https://khronodragon.com/goldmine/faq>\n" +
                    "It saves you, me, and everyone else a lot of time.\n" +
                    "\n" +
                    "*If you like " + Bot.NAME + ", please help keep it alive by donating here: <https://patreon.com/kdragon>.\n" +
                    "Any amount is appreciated.*";

    // load of IDs
    private static final long ANNOUNCEMENT_CHANNEL = 256647384656904192L;
    private static final long PRODUCTION_USER_ID = 239775420470394897L;
    private static final long HOME_GUILD_ID = 239772188649979904L;

    private static volatile int sTries = 0;

    public CoreCog(Bot bot) {
        super(bot);

        if (bot.jda.getSelfUser().getIdLong() == PRODUCTION_USER_ID &&
                !Bot.NAME.equals(bot.jda.getSelfUser().getName()) &&
                bot.getShardNum() == 1) {
            bot.jda.getSelfUser().getManager().setName(Bot.NAME).queue();
        }
    }

    public String getName() {
        return "Core";
    }
    public String getDescription() {
        return "The core, essential cog to keep the bot running.";
    }

    @Command(name = "say", desc = "Say something! Say it!", aliases = {"echo"}, usage = "[message]")
    public void cmdSay(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need text to say!");
            return;
        }

        ctx.send(ctx.rawArgs).queue();
    }

    @Command(name = "test", desc = "Make sure I work.")
    public void cmdTest(Context ctx) {
        ctx.message.addReaction("\uD83D\uDC4D").queue();
    }

    @Command(name = "ping", desc = "Pong!")
    public void cmdPing(Context ctx) {
        String msg = "🏓 WebSockets: " + ctx.jda.getPing() + "ms";
        long beforeTime = System.currentTimeMillis();

        ctx.send(msg).queue(
                message1 -> message1.editMessage(msg + ", message: calculating...").queue(message2 -> {
            double msgPing = (System.currentTimeMillis() - beforeTime) / 2.0;
            message2.editMessage(msg + ", message: " + msgPing + "ms").queue();
        }));
    }

    @Command(name = "faq", desc = "Get the Frequently Asked Questions list.")
    public void cmdFaq(Context ctx) {
        ctx.send("You can find the FAQ at <https://khronodragon.com/goldmine/faq>.").queue();
    }

    @Command(name = "owner", desc = "Become the bot owner.", aliases = {"bot_owner"})
    public void cmdOwnerInfo(Context ctx) {
        ctx.send("My owner is **" + getTag(bot.owner) +
                "**. The bot owner is a role that applies globally to the entire bot, and is the person who owns the actual bot, **not** the owner of a server or anything like that.\n" +
        "**No**, you may not have bot owner, because it allows full bot control. In your server, being **server owner** is sufficient, and grants you permission to perform all the actions you will need to, automatically.\n\n" +
        "**__TL;DR Server owner is enough, you can't have bot owner because that's one person (the actual owner of the bot) and offers unlimited control.__**").queue();
    }

    @Command(name = "help", desc = "Because we all need help.", usage = "{commands and/or cogs}",
            aliases = {"phelp", "halp", "commands"}, thread = true)
    public void cmdHelp(Context ctx) {
        int charLimit = ctx.jda.getSelfUser().isBot() ? MessageEmbed.EMBED_MAX_LENGTH_BOT : MessageEmbed.EMBED_MAX_LENGTH_CLIENT;
        boolean sendPublic = false;
        boolean isOwner = ctx.author.getIdLong() == bot.owner.getIdLong();

        if (ctx.invoker.charAt(0) == 'p' && Permissions.check(ctx,
                Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS,
                Permission.MESSAGE_MANAGE, Permission.MANAGE_SERVER)) {
            sendPublic = true;
        }

        List<MessageEmbed> pages = new ArrayList<>();
        Map<String, List<String>> fields = new HashMap<>();

        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor("Bot Help", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setColor(randomColor());

        if (ctx.args.length < 1) {
            for (com.khronodragon.bluestone.Command cmd: new HashSet<>(bot.commands.values())) {
                if ((!cmd.hidden || isOwner) && !(cmd.requiresOwner && !isOwner)) {
                    String cName = cmd.cog.getCosmeticName();
                    String entry = "\u2022 **" + cmd.name + "**: " + cmd.description;

                    if (fields.containsKey(cName)) {
                        fields.get(cName).add(entry);
                    } else {
                        final LinkedList<String> newList = new LinkedList<>();
                        newList.add(entry);

                        fields.put(cName, newList);
                    }
                }
            }
        } else {
            for (int i = 0; i < ctx.args.length && i < 24; i++) {
                String item = ctx.args.get(i);
                String litem = item.toLowerCase();
                boolean done = false;

                if (bot.cogs.containsKey(item)) {
                    Cog cog = bot.cogs.get(item);

                    for (com.khronodragon.bluestone.Command cmd: new HashSet<>(bot.commands.values())) {
                        if (cmd.cog == cog && (!cmd.hidden || isOwner) && !(cmd.requiresOwner && !isOwner)) {
                            String cName = cmd.cog.getCosmeticName();
                            String entry = "\u2022 **" + cmd.name + "**: " + cmd.description;

                            if (fields.containsKey(cName)) {
                                fields.get(cName).add(entry);
                            } else {
                                final LinkedList<String> newList = new LinkedList<>();
                                newList.add(entry);

                                fields.put(cName, newList);
                            }
                        }
                    }
                    done = true;
                }

                if (bot.commands.containsKey(litem)) {
                    com.khronodragon.bluestone.Command cmd = bot.commands.get(litem);
                    StringBuilder field = new StringBuilder("`");

                    if (cmd.aliases.length < 1) {
                        field.append(ctx.prefix)
                                .append(cmd.name);
                    } else {
                        field.append(ctx.prefix)
                                .append(cmd.name)
                                .append('/')
                                .append(String.join("/", cmd.aliases));
                    }

                    field.append(' ')
                            .append(cmd.usage)
                            .append("`\n\n")
                            .append(cmd.description);
                    fields.put(litem, Collections.singletonList(field.toString()));
                    done = true;
                }

                if (!done) {
                    fields.put(item, Collections.singletonList("Not found."));
                }
            }
        }

        int chars = embedAuthorChars(ctx);
        for (String cog: fields.keySet()) {
            List<String> field = fields.get(cog);
            String content = String.join("\n", field);
            if (content.length() < 1) {
                content = "No visible commands.";
            }

            int preLen = content.length() + cog.length();
            if (chars + preLen > charLimit) {
                pages.add(emb.build());
                emb = newEmbedWithAuthor(ctx)
                        .setColor(randomColor());
                chars = embedAuthorChars(ctx);
            }

            if (content.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
                emb.addField(cog, content, false);
            } else {
                Paginator pager = new Paginator(1024);
                for (String s: field) pager.addLine(s);

                for (String page: pager.getPages()) {
                    emb.addField(cog, page, true);
                }
            }
            chars += preLen;
        }
        pages.add(emb.build());

        MessageDestination destination = MessageDestination.AUTHOR;
        if (sendPublic || bot.isSelfbot()) {
            destination = MessageDestination.CHANNEL;
        } else {
            if (pages.size() < 2 && pages.get(0).getLength() < 1012) {
                destination = MessageDestination.CHANNEL;
            }
        }
        MessageChannel channel = destination.getChannel(ctx);

        for (MessageEmbed page: pages) {
            channel.sendMessage(page).queue(null, exp -> {
                if (exp instanceof ErrorResponseException) {
                    if (((ErrorResponseException) exp).getErrorCode() != 50007) {
                        RestAction.DEFAULT_FAILURE.accept(exp);
                    } else {
                        ctx.send(Emotes.getFailure() +
                                " I couldn't send you help! Make sure you haven't blocked me, and have direct messages from this server turned on.").queue();
                    }
                }
            });
        }

        if (destination == MessageDestination.AUTHOR && ctx.guild != null) {
            try {
                ctx.message.addReaction("✅").queue();
            } catch (PermissionException ignored) {
                ctx.success("Check your DMs!");
            }
        }
    }

    private int embedAuthorChars(Context ctx) {
        if (ctx.guild != null) {
            return ctx.guild.getSelfMember().getEffectiveName().length();
        } else {
            return ctx.jda.getSelfUser().getName().length();
        }
    }

    @Command(name = "uptime", desc = "Get how long I've been running.", aliases = {"memory", "ram"})
    public void cmdUptime(Context ctx) {
        ctx.send("I've been up for **" + bot.formatUptime() +
                "**, and am using **" + Strings.formatMemory() + "** of memory.").queue();
    }

    @EventHandler
    public void onJoin(GuildJoinEvent event) {
        TextChannel defChan = defaultWritableChannel(event.getGuild().getSelfMember());

        if (defChan == null || !defChan.canTalk()) {
            event.getGuild().getOwner().getUser().openPrivateChannel()
                    .queue(ch -> ch.sendMessage(JOIN_MESSAGE).queue());
        } else {
            defChan.sendMessage(JOIN_MESSAGE).queue();
        }
    }
}