package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.enums.MessageDestination;
import com.khronodragon.bluestone.util.Paginator;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.*;

public class CoreCog extends Cog {
    private static final String JOIN_MESSAGE =
            "By adding this bot, you agree that the activity of all users in this server *may* be logged, depending on features used or enabled..\n" +
                    "Features that may log data: quotes, starboard, etc. (this is to comply with the Discord ToS.)\n\n" +
                    "**Enjoy this bot!**\n" +
                    "\n" +
                    "If you ever have questions, *please* read the **FAQ** first: <https://khronodragon.com/goldmine/faq>\n" +
                    "It saves everyone a lot of time.\n" +
                    "\n" +
                    "*If you like Goldmine, please help keep it alive by donating here: <https://patreon.com/kdragon>.\n" +
                    "Any amount is appreciated.*";
    private static final String[] phelpPerms = {"manageChannel", "managePermissions", "messageManage", "manageServer"};
    public CoreCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Core";
    }
    public String getDescription() {
        return "The core, essential cog to keep the bot running.";
    }

    @Command(name = "say", desc = "Say something! Say it!", aliases = {"echo"}, usage = "[message]")
    public void cmdSay(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need text to say!").queue();
            return;
        }

        ctx.send(ctx.rawArgs).queue();
    }

    @Command(name = "test", desc = "Make sure I work.")
    public void cmdTest(Context ctx) {
        ctx.send(ctx.mention + " Everything is looking good! 😄").queue();
    }

    @Command(name = "ping", desc = "Ping, pong!")
    public void cmdPing(Context ctx) {
        String msg = "🏓 WebSockets: " + ctx.jda.getPing() + "ms";
        long beforeTime = System.currentTimeMillis();

        ctx.send(msg).queue(message1 -> {
            message1.editMessage(msg + ", message: calculating...").queue(message2 -> {
                double msgPing = (System.currentTimeMillis() - beforeTime) / 2.0;
                message2.editMessage(msg + ", message: " + msgPing + "ms").queue();
            });
        });
    }

    @Command(name = "help", desc = "Because we all need help.", usage = "{commands and/or cogs}",
            aliases = {"phelp", "halp", "commands", "usage"}, thread = true)
    public void cmdHelp(Context ctx) {
        int charLimit = ctx.jda.getSelfUser().isBot() ? MessageEmbed.EMBED_MAX_LENGTH_BOT : MessageEmbed.EMBED_MAX_LENGTH_CLIENT;
        boolean sendPublic = false;
        boolean isOwner = ctx.author.getIdLong() == bot.owner.getIdLong();
        if (ctx.invoker.startsWith("p") && Permissions.check(phelpPerms, ctx)) {
            sendPublic = true;
        }

        List<MessageEmbed> pages = new ArrayList<>();
        Map<String, List<String>> fields = new HashMap<>();

        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor("Bot Help", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setColor(randomColor());

        if (ctx.args.size() < 1) {
            for (com.khronodragon.bluestone.Command cmd: new HashSet<>(bot.commands.values())) {
                if (!cmd.hidden && !(cmd.requiresOwner && !isOwner)) {
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
            for (String item: ctx.args.subList(0, Math.min(24, ctx.args.size()))) {
                String litem = item.toLowerCase();
                boolean done = false;

                if (bot.cogs.containsKey(item)) {
                    Cog cog = bot.cogs.get(item);

                    for (com.khronodragon.bluestone.Command cmd: new HashSet<>(bot.commands.values())) {
                        if (cmd.cog == cog && !cmd.hidden && !(cmd.requiresOwner && !isOwner)) {
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
                        field.append(cmd.name);
                    } else {
                        field.append(ctx.prefix)
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
                ctx.send(Emotes.getSuccess() + " Check your DMs!").queue();
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

    @Command(name = "uptime", desc = "Get my uptime and memory usage.", aliases = {"memory", "ram"})
    public void cmdUptime(Context ctx) {
        ctx.send("I've been up for **" + bot.formatUptime() +
                "**, and am using **" + Bot.formatMemory() + "** of memory.").queue();
    }

    @EventHandler
    public void onJoin(GuildJoinEvent event) {
        TextChannel defChan = event.getGuild().getDefaultChannel();

        if (defChan == null || !defChan.canTalk()) {
            for (TextChannel channel: event.getGuild().getTextChannels()) {
                if (channel.canTalk()) {
                    channel.sendMessage(JOIN_MESSAGE).queue();
                    return;
                }
            }

            event.getGuild().getOwner().getUser().openPrivateChannel().queue(ch -> {
                ch.sendMessage(JOIN_MESSAGE).queue();
            });
        } else {
            defChan.sendMessage(JOIN_MESSAGE).queue();
        }
    }
}