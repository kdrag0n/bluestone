package com.khronodragon.bluestone.cogs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.TableUtils;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.sql.Starboard;
import com.khronodragon.bluestone.sql.StarboardEntry;
import com.khronodragon.bluestone.sql.Starrer;
import com.khronodragon.bluestone.util.GraphicsUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;

public class StarboardCog extends Cog {
    private static final Logger logger = LogManager.getLogger(StarboardCog.class);
    private static final String[] MOD_PERMS = {"manageServer", "manageChannel"};
    private static final String[] TOP_3_BADGES = {"🥇", "🥈", "🥉"};
    private static final String NO_COMMAND = ":thinking: **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `create/new {channel name='starboard'}` - create a new starboard here (you may pass a different name)\n" +
            "    \u2022 `age [age]` - set the maximum age for messages to be starred, like `2 weeks`\n" +
            "    \u2022 `lock` - lock the starboard, preventing messages from being added\n" +
            "    \u2022 `unlock` - unlock the starboard\n" +
            "    \u2022 `threshold/min [number of stars]` - set the minimum number of stars required for a message to be starred\n" +
            "    \u2022 `clean [number of stars]` - delete starboard entries without more than [number of stars], out of the past 100\n" +
            "    \u2022 `random` - show a random starred message\n" +
            "    \u2022 `show [message ID]` - show a starred message by its ID, or the ID in the starboard\n" +
            "    \u2022 `stats` - show overall starboard stats\n" +
            "\n" +
            "A message is upvoted by reacting with ⭐. With enough stars, it will appear in the starboard, and become starred.\n" +
            "Setting the star threshold will not remove previously starred messages that don't fit the new threshold. Use the `clean` subcommand for that.\n" +
            "A locked starboard will not accept new entries. However, deleting entries is still possible.\n" +
            "To delete the starboard, just delete the channel.";
    private final Parser timeParser = new Parser();
    private final LoadingCache<Pair<Long, Long>, Message> messageCache = CacheBuilder.newBuilder()
            .maximumSize(84)
            .concurrencyLevel(4)
            .initialCapacity(4)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<Pair<Long, Long>, Message>() {
                @Override
                public Message load(Pair<Long, Long> ids) {
                    try {
                        return bot.getJda().getTextChannelById(ids.getLeft()).getMessageById(ids.getRight()).complete();
                    } catch (ErrorResponseException ignored) {
                        return null;
                    }
                }
            });
    private Dao<Starboard, Long> dao;
    private Dao<StarboardEntry, Long> entryDao;
    private Dao<Starrer, Integer> starrerDao;

    public StarboardCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), Starboard.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard table!", e);
        }

        try {
            dao = DaoManager.createDao(bot.getShardUtil().getDatabase(), Starboard.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard DAO!", e);
        }

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), StarboardEntry.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard entry table!", e);
        }

        try {
            entryDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), StarboardEntry.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard entry DAO!", e);
        }

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), Starrer.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard user table!", e);
        }

        try {
            starrerDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), Starrer.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard user DAO!", e);
        }
    }

    public String getName() {
        return "Starboard";
    }

    public String getDescription() {
        return "A starboard! Upvote messages to get them on the starboard. It's like a leaderboard.";
    }

    private Color starGradientColor(int stars) {
        double percent = Math.min(stars / 13, 1.0);

        return GraphicsUtils.interpolateColors(new Color(0xfffdf7), new Color(0xffc20c), percent);
    }

    private String getStarEmoji(int stars) {
        if (5 > stars && stars >= 0)
            return "⭐";
        else if (10 > stars && stars >= 5)
            return "🌟";
        else if (25 > stars && stars >= 10)
            return "💫";
        else
            return "✨";
    }

    private String renderText(int stars, String channelMention, String messageId) {
        String starCountText = "";
        if (stars > 1)
            starCountText = "**" + stars + "** ";

        return getStarEmoji(stars) + ' ' + starCountText +
                channelMention + " | Message ID: " + messageId;
    }

    @EventHandler
    public void onChannelDelete(TextChannelDeleteEvent event) throws SQLException {
        if (dao.idExists(event.getGuild().getIdLong())) {
            dao.deleteById(event.getGuild().getIdLong());

            DeleteBuilder builder = entryDao.deleteBuilder();
            builder.where()
                    .eq("guildId", event.getGuild().getIdLong());
            builder.delete();
        }
    }

    @EventHandler(threaded = true)
    public void onReactionAdd(GuildMessageReactionAddEvent event) throws SQLException, ExecutionException {
        if (!event.getReactionEmote().getName().equals("⭐")) return;
        if (event.getUser().isBot()) return;

        Starboard starboard = dao.queryForId(event.getGuild().getIdLong());
        if (starboard == null) return;
        if (starboard.isLocked()) return;

        int stars = event.getReaction().getUsers().complete().size();
        if (stars < starboard.getStarThreshold()) return;

        String renderedText = renderText(stars, event.getChannel().getAsMention(), event.getMessageId());

        StarboardEntry entry = entryDao.queryForId(event.getMessageIdLong());
        if (entry == null) {
            Message origMessage = messageCache.get(ImmutablePair.of(event.getChannel().getIdLong(),
                    event.getMessageIdLong()));
            if (event.getUser().getIdLong() == origMessage.getAuthor().getIdLong())
                return;
            if (origMessage.getCreationTime().isBefore(OffsetDateTime.now()
                    .minus(starboard.getMaxAge().getTime(), ChronoUnit.MILLIS))) {
                return;
            }

            EmbedBuilder emb = new EmbedBuilder()
                    .setTimestamp(origMessage.getCreationTime())
                    .setAuthor(origMessage.getMember().getEffectiveName(),
                            null, origMessage.getAuthor().getEffectiveAvatarUrl())
                    .setDescription(origMessage.getRawContent())
                    .setColor(starGradientColor(stars));

            if (origMessage.getEmbeds().size() > 0) {
                MessageEmbed data = origMessage.getEmbeds().get(0);
                if (data.getType() == EmbedType.IMAGE) {
                    emb.setImage(data.getUrl());
                }
            }

            if (origMessage.getAttachments().size() > 0) {
                Message.Attachment attachment = origMessage.getAttachments().get(0);
                String url = attachment.getUrl().substring(attachment.getUrl().length() - 4).toLowerCase();

                if (url.endsWith("png") || url.endsWith("jpeg") || url.endsWith("jpg") ||
                        url.endsWith("bmp") || url.endsWith("gif") || url.endsWith("webp")) {
                    emb.setImage(attachment.getUrl());
                } else {
                    for (Message.Attachment attached: origMessage.getAttachments()) {
                        emb.addField("Attachment", '[' + attached.getFileName() +
                                "](" + attached.getUrl() + ')', false);
                    }
                }
            }

            long botMessageId = event.getGuild().getTextChannelById(starboard.getChannelId())
                    .sendMessage(new MessageBuilder()
                            .append(renderedText)
                            .setEmbed(emb.build())
                            .build()).complete().getIdLong();

            entry = new StarboardEntry(event.getMessageIdLong(), event.getGuild().getIdLong(), botMessageId,
                    starboard.getChannelId(), origMessage.getAuthor().getIdLong(),
                    origMessage.getChannel().getIdLong(), stars);
            Starrer starrer = new Starrer(event.getGuild().getIdLong(),
                    event.getUser().getIdLong(), event.getMessageIdLong());
            entryDao.create(entry);
            starrerDao.createOrUpdate(starrer);
        } else {
            entry.setStars(stars);
            entryDao.update(entry);

            Starrer starrer = new Starrer(event.getGuild().getIdLong(),
                    event.getUser().getIdLong(), event.getMessageIdLong());
            starrerDao.createOrUpdate(starrer);

            Message message = messageCache.get(ImmutablePair.of(starboard.getChannelId(), entry.getBotMessageId()));
            message.editMessage(new MessageBuilder()
                    .append(renderedText)
                    .setEmbed(new EmbedBuilder(message.getEmbeds().get(0))
                            .setColor(starGradientColor(stars))
                            .build())
                    .build()).queue();
        }
    }

    @EventHandler(threaded = true)
    public void onReactionRemove(GuildMessageReactionRemoveEvent event) throws SQLException, ExecutionException {
        if (!event.getReactionEmote().getName().equals("⭐"))
            return;

        int stars = event.getReaction().getUsers().complete().size();

        Starboard starboard = dao.queryForId(event.getGuild().getIdLong());
        if (starboard == null) return;
        if (stars < starboard.getStarThreshold())
            messageDelete(event.getMessageIdLong());

        StarboardEntry entry = entryDao.queryForId(event.getMessageIdLong());
        if (entry != null) {
            entry.setStars(stars);
            entryDao.update(entry);

            DeleteBuilder builder = starrerDao.deleteBuilder();
            builder.where()
                    .eq("userId", event.getUser().getIdLong())
                    .and()
                    .eq("messageId", entry.getMessageId());
            builder.delete();

            String renderedText = renderText(stars, event.getChannel().getAsMention(), event.getMessageId());
            messageCache.get(ImmutablePair.of(starboard.getChannelId(), entry.getBotMessageId()))
                    .editMessage(renderedText).queue();
        }
    }

    @EventHandler(threaded = true)
    public void onReactionRemoveAll(GuildMessageReactionRemoveAllEvent event) throws SQLException, ExecutionException {
        messageDelete(event.getMessageIdLong());
    }

    @EventHandler(threaded = true)
    public void onMessageDelete(GuildMessageDeleteEvent event) throws SQLException, ExecutionException {
        messageDelete(event.getMessageIdLong());
    }

    @EventHandler(threaded = true)
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) throws SQLException, ExecutionException {
        for (String sid: event.getMessageIds()) {
            long id = Long.parseUnsignedLong(sid);
            StarboardEntry entry = entryDao.queryBuilder()
                    .where()
                    .eq("botMessageId", id)
                    .or()
                    .eq("messageId", id)
                    .queryForFirst();

            if (entry != null)
                messageDelete(entry);
        }
    }

    private void messageDelete(long messageId) throws SQLException, ExecutionException {
        StarboardEntry entry = entryDao.queryForId(messageId);
        if (entry != null) {
            messageDelete(entry);
        }
    }

    private void messageDelete(StarboardEntry entry) throws SQLException, ExecutionException {
        Message message = null;
        try {
            message = messageCache.get(ImmutablePair.of(entry.getBotChannelId(), entry.getBotMessageId()));
        } catch (UncheckedExecutionException ignored) {}
        entryDao.delete(entry);

        DeleteBuilder builder = starrerDao.deleteBuilder();
        builder.where()
                .eq("messageId", entry.getMessageId());
        builder.delete();
        logger.info(builder.prepare().getStatement());

        if (message != null)
            message.delete()
                    .reason("All reactions were deleted on source message, or source message itself was deleted")
                    .queue(null, e -> {});
    }

    private Starboard requireStarboard(Context ctx) throws PassException, SQLException {
        Starboard board = dao.queryForId(ctx.guild.getIdLong());
        if (board == null) {
            ctx.send("🚫 You must create a starboard first to use this command!").queue();
            throw new PassException();
        }

        return board;
    }

    @Command(name = "star", desc = "Master starboard management command.", aliases = {"stars", "starboard", "starman"},
            thread = true, guildOnly = true)
    public void managementCommand(Context ctx) throws Throwable {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        switch (invoked) {
            case "create":
            case "new":
                cmdCreate(ctx);
                break;
            case "age":
                cmdAge(ctx);
                break;
            case "lock":
                cmdLock(ctx);
                break;
            case "unlock":
                cmdUnlock(ctx);
                break;
            case "limit":
            case "threshold":
            case "min":
                cmdThreshold(ctx);
                break;
            case "clean":
                cmdClean(ctx);
                break;
            case "random":
                cmdRandom(ctx);
                break;
            case "show":
                cmdShow(ctx);
                break;
            case "stats":
                cmdStats(ctx);
                break;
            default:
                ctx.send(NO_COMMAND).queue();
                break;
        }
    }

    private void cmdCreate(Context ctx) throws SQLException {
        com.khronodragon.bluestone.Command.checkPerms(ctx, MOD_PERMS);

        Starboard oldBoard = dao.queryForId(ctx.guild.getIdLong());
        if (oldBoard != null) {
            ctx.send(Emotes.getFailure() + " This server already has a starboard! <#" +
                    oldBoard.getChannelId() + '>').queue();
            return;
        }

        String channelName = "starboard";
        if (ctx.args.size() > 1) {
            String wantedName = ctx.args.get(1);
            if (!wantedName.matches("^[a-zA-Z0-9_-]{2,100}$")) {
                ctx.send(Emotes.getFailure() + " Channel name must be between 2 and 100 characters long!").queue();
                return;
            }

            channelName = wantedName.toLowerCase();
        }

        TextChannel channel = (TextChannel) ctx.guild.getController().createTextChannel(channelName)
                .setTopic("Starboard created and managed by " + ctx.guild.getSelfMember().getAsMention())
                .addPermissionOverride(ctx.guild.getPublicRole(),
                        ImmutableList.of(Permission.MESSAGE_HISTORY),
                        ImmutableList.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(ctx.guild.getSelfMember(),
                        ImmutableList.of(Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE,
                                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY,
                                Permission.MESSAGE_READ),
                        null).complete();

        Starboard starboard = new Starboard(channel.getIdLong(), ctx.guild.getIdLong(), 1, false);
        dao.create(starboard);

        ctx.send("✨ Created " + channel.getAsMention() +
                " with a maximum message age of 1 week, and minimum star count of 1.").queue();
    }

    private void cmdAge(Context ctx) throws SQLException {
        com.khronodragon.bluestone.Command.checkPerms(ctx, MOD_PERMS);

        if (ctx.rawArgs.length() < 5) {
            ctx.send(Emotes.getFailure() + " I need an age to set, like `2 days` or `1 year`!").queue();
            return;
        }
        Starboard starboard = requireStarboard(ctx);

        Date date = null;
        for (DateGroup group: timeParser.parse(ctx.rawArgs.substring(3))) {
            if (!group.getDates().isEmpty()) {
                date = group.getDates().get(0);
            }
        }

        if (date == null) {
            ctx.send(Emotes.getFailure() + " Invalid age! Formats like `2 days` or `1 year` will work.").queue();
            return;
        }

        starboard.setMaxAge(new Date(date.getTime() - System.currentTimeMillis()));
        dao.update(starboard);
        ctx.send("🌙 Maximum starred message age set.").queue();
    }

    private void cmdLock(Context ctx) throws SQLException {
        com.khronodragon.bluestone.Command.checkPerms(ctx, MOD_PERMS);

        Starboard starboard = requireStarboard(ctx);

        if (starboard.isLocked()) {
            ctx.send(Emotes.getFailure() + " Starboard is already locked!").queue();
        } else {
            starboard.setLocked(true);
            dao.update(starboard);
            ctx.send(Emotes.getSuccess() + " Starboard locked.").queue();
        }
    }

    private void cmdUnlock(Context ctx) throws SQLException {
        com.khronodragon.bluestone.Command.checkPerms(ctx, MOD_PERMS);

        Starboard starboard = requireStarboard(ctx);

        if (starboard.isLocked()) {
            starboard.setLocked(false);
            dao.update(starboard);
            ctx.send(Emotes.getSuccess() + " Starboard unlocked.").queue();
        } else {
            ctx.send(Emotes.getFailure() + " Starboard isn't locked!").queue();
        }
    }

    private void cmdThreshold(Context ctx) throws SQLException {
        com.khronodragon.bluestone.Command.checkPerms(ctx, MOD_PERMS);

        Starboard starboard = requireStarboard(ctx);
        if (ctx.args.size() < 2) {
            ctx.send(Emotes.getFailure() + " I need a number to set the minimum star count to!").queue();
            return;
        }

        int newThreshold;
        try {
            newThreshold = Integer.parseInt(ctx.args.get(1));
        } catch (NumberFormatException ignored) {
            ctx.send(Emotes.getFailure() + " Invalid number!").queue();
            return;
        }

        starboard.setStarThreshold(newThreshold);
        dao.update(starboard);
        ctx.send(Emotes.getSuccess() + " Minimum star count set.").queue();
    }

    private void cmdClean(Context ctx) throws SQLException {
        Starboard starboard = requireStarboard(ctx);
        if (ctx.args.size() < 2) {
            ctx.send(Emotes.getFailure() + " I need a required star count to clean with!").queue();
            return;
        }

        int required;
        try {
            required = Integer.parseInt(ctx.args.get(1));
        } catch (NumberFormatException ignored) {
            ctx.send(Emotes.getFailure() + " Invalid number!").queue();
            return;
        }

        TextChannel channel = ctx.guild.getTextChannelById(starboard.getChannelId());

        channel.deleteMessagesByIds(entryDao.queryBuilder()
                .orderBy("messageId", false)
                .limit(100L)
                .where()
                .eq("guildId", starboard.getGuildId())
                .and()
                .lt("stars", required + 1)
                .query()
                .stream()
                .map(e -> Long.toUnsignedString(e.getBotMessageId()))
                .collect(Collectors.toList())).complete();

        ctx.send(Emotes.getSuccess() + " Starboard cleaned.").queue();
    }

    private void cmdRandom(Context ctx) throws Throwable {
        Starboard starboard = requireStarboard(ctx);

        List<StarboardEntry> entry = entryDao.queryBuilder()
                .orderByRaw("RAND()")
                .limit(1L)
                .where()
                .eq("guildId", starboard.getGuildId())
                .query();

        if (entry.size() < 1)
            ctx.send(Emotes.getFailure() + " The starboard is empty!").queue();
        else {
            try {
                ctx.send(messageCache.get(ImmutablePair.of(starboard.getChannelId(),
                        entry.get(0).getBotMessageId()))).queue();
            } catch (ExecutionException bE) {
                Throwable e = bE.getCause();

                if (e instanceof ErrorResponseException)
                    ctx.send(Emotes.getFailure() + " Somehow...the entry's message was deleted?").queue();
                else
                    throw e;
            }
        }
    }

    private void cmdShow(Context ctx) throws SQLException, ExecutionException {
        Starboard starboard = requireStarboard(ctx);

        long messageId;
        try {
            messageId = MiscUtil.parseSnowflake(ctx.args.get(1));
        } catch (NumberFormatException ignored) {
            ctx.send(Emotes.getFailure() + " Invalid message ID!").queue();
            return;
        } catch (IndexOutOfBoundsException ignored) {
            ctx.send(Emotes.getFailure() +
                    " I need the ID of either a starred message, or a bot message in <#" +
                    starboard.getChannelId() + ">!").queue();
            return;
        }

        List<StarboardEntry> result = entryDao.queryBuilder()
                .limit(1L)
                .where()
                .eq("guildId", starboard.getGuildId())
                .and()
                .eq("messageId", messageId)
                .or()
                .eq("botMessageId", messageId)
                .query();

        if (result.size() < 1) {
            ctx.send(Emotes.getFailure() + " No such starred message, or bot message in starboard!").queue();
            return;
        }

        ctx.send(messageCache.get(ImmutablePair.of(starboard.getChannelId(), result.get(0).getBotMessageId()))).queue();
    }

    private void cmdStats(Context ctx) throws SQLException {
        Starboard starboard = requireStarboard(ctx);
        List<StarboardEntry> starCountEntries = entryDao.queryBuilder()
                .selectColumns("stars")
                .where()
                .eq("guildId", starboard.getGuildId())
                .query();

        if (starCountEntries.size() < 1) {
            ctx.send(Emotes.getFailure() + " There needs to be at least 1 starred message here!").queue();
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(val(ctx.member.getColor()).or(Color.WHITE))
                .setFooter("Tracking stars and stargazers since", ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setTimestamp(MiscUtil.getCreationTime(starboard.getChannelId()))
                .setTitle("Starboard Stats");
        StringBuilder desc = emb.getDescriptionBuilder();
        desc.append("There are ")
                .append(starCountEntries.size())
                .append(" starred messages, with a total of ");

        int totalStars = 0;
        for (StarboardEntry entry: starCountEntries)
            totalStars += entry.getStars();

        desc.append(totalStars)
                .append(" stars across all messages.");

        StringBuilder top3entriesBuilder = new StringBuilder();
        List<StarboardEntry> top3entries = entryDao.queryBuilder()
                .selectColumns("messageId", "stars")
                .orderBy("stars", false)
                .limit(3L)
                .where()
                .eq("guildId", starboard.getGuildId())
                .query();
        for (int i = 0; i < top3entries.size(); i++) {
            StarboardEntry entry = top3entries.get(i);
            top3entriesBuilder.append(TOP_3_BADGES[i])
                    .append(' ')
                    .append(entry.getMessageId())
                    .append(' ')
                    .append('(')
                    .append(entry.getStars())
                    .append(" star")
                    .append(entry.getStars() == 1 ? '\00' : 's')
                    .append(")\n");
        }
        emb.addField("Top Starred Messages", top3entriesBuilder.toString(), false);

        StringBuilder top3recvBuilder = new StringBuilder();
        List<StarboardEntry> top3recvRaw = entryDao.queryBuilder()
                .orderBy("stars", false)
                .selectColumns("authorId", "stars")
                .where()
                .eq("guildId", starboard.getGuildId())
                .query();
        List<Pair<Long, Integer>> top3recv = top3recvRaw.stream()
                .map(StarboardEntry::getAuthorId)
                .distinct()
                .map(id -> ImmutablePair.of(id, top3recvRaw.stream()
                        .filter(e -> e.getAuthorId() == id)
                        .mapToInt(StarboardEntry::getStars)
                        .sum()))
                .sorted(Collections.reverseOrder(Comparator.comparing(Pair::getRight)))
                .limit(3)
                .collect(Collectors.toList());
        for (int i = 0; i < top3recv.size(); i++) {
            Pair<Long, Integer> entry = top3recv.get(i);
            top3recvBuilder.append(TOP_3_BADGES[i])
                    .append(" <@")
                    .append(entry.getLeft())
                    .append("> (")
                    .append(entry.getRight())
                    .append(" star")
                    .append(entry.getRight() == 1 ? '\00' : 's')
                    .append(")\n");
        }
        emb.addField("Top Stargazers", top3recvBuilder.toString(), false);

        StringBuilder top3sendBuilder = new StringBuilder();
        List<Starrer> top3sendRaw = starrerDao.queryBuilder()
                .selectColumns("userId")
                .where()
                .eq("guildId", starboard.getGuildId())
                .query();
        List<Pair<Long, Integer>> top3send = top3sendRaw.stream()
                .map(Starrer::getUserId)
                .distinct()
                .map(id -> ImmutablePair.of(id, top3sendRaw.stream()
                        .filter(s -> s.getUserId() == id)
                        .mapToInt(ignored -> 1)
                        .sum()))
                .sorted(Collections.reverseOrder(Comparator.comparing(Pair::getRight)))
                .limit(3)
                .collect(Collectors.toList());
        for (int i = 0; i < top3send.size(); i++) {
            Pair<Long, Integer> entry = top3send.get(i);
            top3sendBuilder.append(TOP_3_BADGES[i])
                    .append(" <@")
                    .append(entry.getLeft())
                    .append("> (")
                    .append(entry.getRight())
                    .append(" star")
                    .append(entry.getRight() == 1 ? '\00' : 's')
                    .append(")\n");
        }
        emb.addField("Top Star Givers", top3sendBuilder.toString(), false);

        ctx.send(emb.build()).queue();
    }
}
