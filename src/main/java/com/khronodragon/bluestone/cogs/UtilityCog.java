package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.ShardUtil;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.util.RegexUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.apache.commons.lang3.text.WordUtils;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.str;
import static java.text.MessageFormat.format;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilityCog extends Cog {
    private static final Pattern UNICODE_EMOTE_PATTERN = Pattern.compile("([\\x{10000}-\\x{10ffff}\\u2615])");
    private static final Pattern CUSTOM_EMOTE_PATTERN = Pattern.compile("<:[a-z_]+:([0-9]{17,19})>", Pattern.CASE_INSENSITIVE);
    private static final String RED_TICK = "<:redTick:312314733816709120>";
    private static final String GREEN_TICK = "<:greenTick:312314752711786497>";
    public UtilityCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Utility";
    }
    public String getDescription() {
        return "Essential utility commands, as well as playful ones.";
    }

    @Command(name = "icon", desc = "Get the current guild's icon.", guildOnly = true)
    public void cmdIcon(Context ctx) {
        ctx.send(val(ctx.guild.getIconUrl()).or("There's no icon here!")).queue();
    }

    @Command(name = "user", desc = "Get some info about a user.", usage = "{user}", aliases = {"userinfo", "whois"})
    public void cmdUser(Context ctx) { // TODO: parseUser
        ctx.send("java is terrible so this command is impossible to implement").queue();
    }

    @Command(name = "guildinfo", desc = "Get loads of info about this guild.", guildOnly = true, aliases = {"ginfo", "guild", "server", "serverinfo", "sinfo"})
    public void cmdGuildInfo(Context ctx) {
        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(ctx.guild.getName(), null,
                        val(ctx.guild.getIconUrl()).or(ctx.jda.getSelfUser().getEffectiveAvatarUrl()))
                .setFooter(ctx.guild.getSelfMember().getEffectiveName(), ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .addField("ID", ctx.guild.getId(), true)
                .addField("Members", str(ctx.guild.getMembers().size()), true)
                .addField("Channels", str(ctx.guild.getTextChannels().size() + ctx.guild.getVoiceChannels().size()), true)
                .addField("Roles", str(ctx.guild.getRoles().size()) ,true)
                .addField("Emotes", str(ctx.guild.getEmotes().size()), true)
                .addField("Region", ctx.guild.getRegion().getName(), true)
                .addField("Owner", ctx.guild.getOwner().getAsMention(), true)
                .addField("Default Channel", ctx.guild.getPublicChannel().getAsMention(), true)
                .addField("Admins Need 2FA?", ctx.guild.getRequiredMFALevel().getKey() == 1 ? "Yes" : "No", true)
                .addField("Content Scan Level", ctx.guild.getExplicitContentLevel().getDescription(), true)
                .addField("Verification Level", WordUtils.capitalize(ctx.guild.getVerificationLevel().name().toLowerCase()
                        .replace('_', ' ')), true)
                .setThumbnail(ctx.guild.getIconUrl());

        ctx.send(emb.build()).queue();
    }

    @Command(name = "info", desc = "Get some info about me.", aliases = {"about", "stats", "statistics"})
    public void cmdInfo(Context ctx) {
        Runtime runtime = Runtime.getRuntime();
        ShardUtil shardUtil = bot.getShardUtil();
        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/")
                .setColor(randomColor())
                .setDescription("Made by **Dragon5232#1841**")
                .addField("Guilds", str(shardUtil.getGuildCount()), true)
                .addField("Uptime", bot.formatUptime(), true)
                .addField("Requests", str(shardUtil.getRequestCount()), true)
                .addField("Threads", str(Thread.activeCount()), true)
                .addField("Memory Used", bot.formatMemory(), true)
                .addField("Users", str(shardUtil.getUserCount()), true)
                .addField("Channels", str(shardUtil.getChannelCount()), true)
                .addField("Commands", str(new HashSet<>(bot.commands.values()).size()), true);

        if (ctx.jda.getSelfUser().getIdLong() == 239775420470394897L) {
            emb.addField("Invite Link", "https://tiny.cc/goldbot", true);
        }

        ctx.send(emb.build()).queue();
    }

    @Command(name = "invite", desc = "Generate an invite link for myself or another bot.", aliases = {"addbot"})
    public void cmdInvite(Context ctx) {
        if (ctx.rawArgs.length() == 0) {
            ctx.send(format("<https://discordapp.com/api/oauth2/authorize?client_id={0}&scope=bot&permissions={1}>",
                    ctx.jda.getSelfUser().getId(), "1609825363")).queue();
        } else {
            if (!ctx.rawArgs.matches("^[0-9]{16,20}$")) {
                ctx.send(":warning: Invalid ID!").queue();
                return;
            }

            ctx.send(format("<https://discordapp.com/api/oauth2/authorize?client_id={0}&scope=bot&permissions=3072>",
                    ctx.rawArgs)).queue();
        }
    }

    @Command(name = "home", desc = "Get my \"contact\" info.", aliases = {"website", "web"})
    public void cmdHome(Context ctx) {
        ctx.send("**Author\\'s Website**: <https://khronodragon.com>\n" +
                "**Forums**: <https://forums.khronodragon.com>\n" +
                "**Short Invite Link**: <https://tiny.cc/goldbot>\n" +
                "**Support Guild**: <https://discord.gg/dwykTHc>").queue();
    }

    private Runnable pollTask(Set<String> validEmotes, long messageId, Map<String, Set<User>> pollTable) {
        return () -> {
            while (true) {
                MessageReactionAddEvent event = bot.waitForReaction(-1, ev -> ev.getMessageIdLong() == messageId &&
                        ev.getUser().getIdLong() != bot.getJda().getSelfUser().getIdLong() &&
                        validEmotes.contains(ev.getReactionEmote().toString()));
                pollTable.get(event.getReactionEmote().toString()).add(event.getUser());
            }
        };
    }

    private Set<Emote> customEmoteParser(Guild guild, Stream<String> rawEmoteIdStream) {
        return rawEmoteIdStream
                .map(guild::getEmoteById)
                .collect(Collectors.toSet());
    }

    @Command(name = "poll", desc = "Start a poll, with reactions.", usage = "[emotes] [question] [time in minutes]", guildOnly = true)
    public void cmdPoll(Context ctx) {
        if (ctx.args.size() == 0) {
            ctx.send(":warning: Missing question, emotes, and time (in minutes)!").queue();
            return;
        }

        long pollTime;
        try {
            pollTime = Long.parseUnsignedLong(ctx.args.get(ctx.args.size() - 1));
        } catch (NumberFormatException e) {
            ctx.send(":warning: Invalid time! Time is given as integer minutes.").queue();
            return;
        }
        ctx.args.remove(ctx.args.size() - 1);

        String preQuestion = String.join(" ", ctx.args);
        Set<Character> unicodeEmotes = RegexUtil.matchStream(UNICODE_EMOTE_PATTERN, preQuestion)
                                        .map(match -> match.group().charAt(0)).collect(Collectors.toSet());
        Set<Emote> customEmotes = customEmoteParser(ctx.guild, RegexUtil.matchStream(CUSTOM_EMOTE_PATTERN, preQuestion)
                                                                .map(MatchResult::group));
        if (customEmotes.contains(null)) {
            ctx.send(":warning: You provided an invalid custom emote!").queue();
            return;
        } else if (unicodeEmotes.size() + customEmotes.size() < 2) {
            ctx.send(":warning: You need at least 2 emotes to poll!").queue();
            return;
        }

        final String question = preQuestion.replaceAll(UNICODE_EMOTE_PATTERN.pattern(), "")
                           .replaceAll("\\s+", " ").trim();

        Map<String, Set<User>> pollTable = new HashMap<>();
        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor(ctx.member.getEffectiveName() + "is polling...", null, ctx.author.getEffectiveAvatarUrl())
                                .setColor(ctx.member.getColor())
                                .setDescription(question)
                                .appendDescription("\n\n")
                                .appendDescription("**Reactions are being added...**");

        ctx.send(embed.build()).queue(msg -> {
            for (char emote: unicodeEmotes) {
                msg.addReaction(String.valueOf(emote)).queue();
            }
            for (Emote emote: customEmotes) {
                msg.addReaction(emote).queue();
            }

            Thread pollThread = new Thread(pollTask(Stream.concat(unicodeEmotes.stream().map(String::valueOf),
                                                    customEmotes.stream().map(Emote::toString)).collect(Collectors.toSet()),
                                                    msg.getIdLong(), pollTable));
            pollThread.setDaemon(true);
            pollThread.setName("Reaction Poll Counter Thread");

            embed.setDescription(question)
                    .appendDescription("\n\n")
                    .appendDescription("**" + GREEN_TICK + " Go ahead and vote!**");

            bot.getScheduledExecutor().schedule(() -> {
                msg.editMessage(embed.build()).queue(newMsg -> {
                    pollThread.start();

                    bot.getScheduledExecutor().schedule(() -> {
                        pollThread.interrupt();
                        Map<String, Integer> resultTable = pollTable.entrySet().stream()
                                .collect(Collectors.toMap(
                                entry -> entry.getKey(),
                                entry -> entry.getValue().size()
                        ));
                        String winner = resultTable.entrySet().stream()
                                .max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1)
                                .get().getKey();
                        List<String> orderedResultList = new LinkedList<>();

                        embed.setDescription(question)
                                .appendDescription("\n\n")
                                .appendDescription("**" + RED_TICK + " Poll ended.**")
                                .addField("Winner", winner, false);
                        newMsg.editMessage(embed.build()).queue();

                        ctx.send("**Poll** `" + question + "` **ended!\n" +
                                    "Winner: " + winner + "**\n\n" +
                                    "Full Results:\n" + String.join("\n", orderedResultList)).queue();;
                    }, pollTime, TimeUnit.SECONDS);//MINUTES); // TODO: use MINUTES for non testing
                });
            }, 180L, TimeUnit.MILLISECONDS);
        });
    }
}
