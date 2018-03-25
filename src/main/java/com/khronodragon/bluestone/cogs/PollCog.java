package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.emotes.DiscordEmoteProvider;
import com.khronodragon.bluestone.sql.ActivePoll;
import com.khronodragon.bluestone.util.RegexUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PollCog extends Cog {
    private static final Logger logger = LogManager.getLogger(PollCog.class);

    private static final Pattern UNICODE_EMOTE_PATTERN = Pattern.compile("([\\u20a0-\\u32ff\\x{1f000}-\\x{1ffff}\\x{fe4e5}-\\x{fe4ee}]|[0-9]\\u20e3)");
    private static final Pattern CUSTOM_EMOTE_PATTERN = Pattern.compile("<:[a-z_]+:([0-9]{17,19})>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTIGUOUS_SPACE_PATTERN = Pattern.compile("\\s+");
    private final Dao<ActivePoll, Long> pollDao;

    private final Parser timeParser = new Parser();

    public PollCog(Bot bot) {
        super(bot);

        pollDao = setupDao(ActivePoll.class);

        try {
            scheduleAllPolls();
        } catch (SQLException e) {
            logger.warn("Error rescheduling previous polls", e);
        }
    }

    public String getName() {
        return "Poll";
    }

    public String getDescription() {
        return "Create a public poll.";
    }

    private void scheduleAllPolls() throws SQLException {
        for (ActivePoll poll: pollDao.queryForAll())
            schedulePoll(poll);
    }

    @Command(name = "poll", desc = "Start a reaction poll.", usage = "[emotes] [question] [time]", guildOnly = true)
    public void cmdPoll(Context ctx) {
        if (ctx.args.length < 1) {
            ctx.fail("Missing question, emotes, and time (like `5 minutes`)!");
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(ctx.guild.getTextChannelById(ctx.channel.getIdLong()),
                Permission.MESSAGE_HISTORY)) {
            ctx.fail("I need the **read message history** permission!");
            return;
        }

        StringBuilder qBuilder = new StringBuilder(ctx.rawArgs);
        List<DateGroup> groups = timeParser.parse(ctx.rawArgs);
        Collections.reverse(groups);

        Date date = null;
        for (DateGroup group: groups) {
            if (!group.getDates().isEmpty()) {
                date = group.getDates().get(0);
                int pos = qBuilder.lastIndexOf(group.getText());
                qBuilder.replace(pos, pos + group.getText().length(), "");
                break;
            }
        }

        if (date == null || date.getTime() < System.currentTimeMillis()) {
            ctx.fail("Invalid length! Examples: `1 week`, `5 minutes`, `2 years`");
            return;
        } else if (date.getTime() > System.currentTimeMillis() + 15768000000L) { // 6 months
            ctx.fail("That time/date is too far away!");
            return;
        }

        final Date finalDate = date;
        String preQuestion = qBuilder.toString().trim();

        Set<String> unicodeEmotes = RegexUtils.matchStream(UNICODE_EMOTE_PATTERN, preQuestion)
                .map(MatchResult::group).collect(Collectors.toSet());
        Set<Emote> customEmotes = RegexUtils.matchStream(CUSTOM_EMOTE_PATTERN, preQuestion)
                .map(m -> ctx.jda.getEmoteById(m.group(1)))
                .collect(Collectors.toSet());

        if (customEmotes.contains(null)) {
            customEmotes.remove(null);
        } else if (unicodeEmotes.size() + customEmotes.size() < 2) {
            ctx.fail("You need at least 2 emotes to start a poll!");
            return;
        }

        final Matcher _m = UNICODE_EMOTE_PATTERN.matcher(preQuestion);
        preQuestion = _m.replaceAll("");
        preQuestion = _m.usePattern(DiscordEmoteProvider.CUSTOM_EMOTE_PATTERN).reset(preQuestion).replaceAll("");
        preQuestion = _m.usePattern(CONTIGUOUS_SPACE_PATTERN).reset(preQuestion).replaceAll(" ");
        final String question = preQuestion.trim();
        final Color c = ctx.member.getColor();

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(ctx.member.getEffectiveName() + " is polling...",
                        null, ctx.author.getEffectiveAvatarUrl())
                .setColor(c == null ? Color.WHITE : c)
                .setDescription(question)
                .appendDescription("\n\n")
                .appendDescription("**⌛ Reactions are being added...**");

        ctx.send(embed.build()).queue(msg -> {
            for (String emote: unicodeEmotes) {
                msg.addReaction(emote).queue();
            }

            for (Emote emote: customEmotes) {
                msg.addReaction(emote).queue();
            }

            ActivePoll poll = new ActivePoll(msg.getIdLong(), msg.getChannel().getIdLong(), finalDate);
            Bot.threadExecutor.execute(() -> {
                try {
                    pollDao.create(poll);
                } catch (SQLException e) {
                    logger.error("Error persisting poll", e);
                }
            });

            embed.setDescription(question)
                    .appendDescription("\n\n")
                    .appendDescription("**✅ Vote!**");

            schedulePoll(poll);

            Bot.scheduledExecutor.schedule(() ->
                            msg.editMessage(embed.build()).queue(),
                    (unicodeEmotes.size() + customEmotes.size()) * (int) (ctx.jda.getPing() * 1.92),
                    TimeUnit.MILLISECONDS);
        });
    }

    private void schedulePoll(final ActivePoll poll) {
        long calculatedTime = poll.getEndTime().getTime() - System.currentTimeMillis();

        if (bot.jda.getTextChannelById(poll.getChannelId()) == null)
            return;

        Bot.scheduledExecutor.schedule(() -> {
            TextChannel channel = bot.jda.getTextChannelById(poll.getChannelId());

            try {
                if (channel == null)
                    return;

                Message message;
                try {
                    message = channel.getMessageById(poll.getMessageId()).complete();
                } catch (Exception ignored) {
                    return;
                }

                if (message == null)
                    return;

                long ourId = bot.jda.getSelfUser().getIdLong();
                Map<MessageReaction.ReactionEmote, Integer> resultTable = message.getReactions().stream()
                        .map(r -> ImmutablePair.of(r, (int) r.getUsers()
                                .complete()
                                .stream()
                                .filter(u -> u.getIdLong() != ourId)
                                .count()))
                        .sorted(Collections.reverseOrder(Comparator.comparing(ImmutablePair<MessageReaction, Integer>::getRight)))
                        .collect(Collectors.toMap(
                                e -> e.getLeft().getReactionEmote(),
                                ImmutablePair::getRight,
                                (k, v) -> { throw new IllegalStateException("Duplicate key " + k); },
                                LinkedHashMap::new
                        ));

                MessageReaction.ReactionEmote winnerKey = Collections.max(resultTable.entrySet(), Map.Entry.comparingByValue()).getKey();
                String winner = winnerKey.getEmote() == null ? winnerKey.getName() : winnerKey.getEmote().getAsMention();

                List<String> orderedResultList = resultTable.entrySet().stream()
                        .map(e -> {
                            final MessageReaction.ReactionEmote key = e.getKey();
                            final Integer value = e.getValue();
                            final String userKey = key.getEmote() == null ? key.getName() : key.getEmote().getAsMention();

                            return userKey + ": " + value + " vote" + (value == 1 ? "" : "s");
                        })
                        .collect(Collectors.toList());

                EmbedBuilder emb = new EmbedBuilder(message.getEmbeds().get(0))
                        .addField("Winner", winner, false);
                emb.getDescriptionBuilder().replace(emb.getDescriptionBuilder().indexOf("**✅ Vote!**"),
                        emb.getDescriptionBuilder().length(), "**❌ Poll ended.**");

                message.editMessage(emb.build()).queue();
                channel.sendMessage("**Poll ended!**\n" +
                        "Winner: " + winner + "\n\n" +
                        "Full Results:\n" + String.join("\n", orderedResultList)).queue();
            } catch (Exception e) {
                logger.error("Poll: error", e);
            } finally {
                try {
                    pollDao.delete(poll);
                } catch (SQLException e) {
                    logger.error("Error deleting poll", e);
                }
            }
        }, calculatedTime, TimeUnit.MILLISECONDS);
    }
}
