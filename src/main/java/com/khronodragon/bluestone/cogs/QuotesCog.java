package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.jagrosh.jdautilities.menu.Paginator;
import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.sql.Quote;
import com.khronodragon.bluestone.sql.QuotesBannedMember;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;

public class QuotesCog extends Cog {
    private static final Logger logger = LogManager.getLogger(QuotesCog.class);
    private static final String NO_COMMAND = "🤔 **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `[id]` - show quote `id`\n" +
            "    \u2022 `add [quote]` - add a quote\n" +
            "    \u2022 `delete [id]` - delete a quote, if you own it\n" +
            "    \u2022 `list` - list all the quotes (paginated)\n" +
            "    \u2022 `random` - view a random quote\n" +
            "    \u2022 `count` - see how many quotes there are\n" +
            "    \u2022 `message [message id]` - quote a message by its ID\n" +
            "\n" +
            "Aliases: [create, new, remove], [del, rm], [rand], [num], [addmsg, madd, msg]";
    private final Dao<Quote, Integer> dao;
    private final Dao<QuotesBannedMember, Long> banDao;

    public QuotesCog(Bot bot) {
        super(bot);

        dao = setupDao(Quote.class);
        banDao = setupDao(QuotesBannedMember.class);
    }

    public String getName() {
        return "Quotes";
    }

    public String getDescription() {
        return "Gotta quote 'em all!";
    }

    @Command(name = "quote", desc = "Add, create, or view a quote!", thread = true,
            usage = "[action / id] {args?...}", aliases = {"quotes", "q"})
    public void cmdQuote(Context ctx) throws SQLException {
        if (ctx.args.empty) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        if (invoked.equals("add") || invoked.equals("create") || invoked.equals("new"))
            quoteCmdAdd(ctx);
        else if (invoked.equals("delete") || invoked.equals("remove") ||
                invoked.equals("del") || invoked.equals("rm"))
            quoteCmdDelete(ctx);
        else if (invoked.equals("list"))
            quoteCmdList(ctx);
        else if (invoked.equals("random") || invoked.equals("rand"))
            quoteCmdRandom(ctx);
        else if (invoked.equals("count") || invoked.equals("num"))
            quoteCmdCount(ctx);
        else if (invoked.equals("message") || invoked.equals("addmsg") || invoked.equals("madd") || invoked.equals("msg"))
            quoteCmdAddMessage(ctx);
        else if (Strings.is4Digits(invoked))
            quoteShowId(ctx, Integer.parseInt(invoked));
        else if (Strings.isID(invoked)) {
            ctx._flag = true; // use different argument index
            quoteCmdAddMessage(ctx);
        } else
            ctx.send(NO_COMMAND).queue();
    }

    private void quoteCmdAdd(Context ctx) throws SQLException {
        if (ctx.args.length < 8) {
            ctx.fail("I need valid text to quote!");
            return;
        } else if (banDao.queryForId(ctx.author.getIdLong()) != null) {
            ctx.fail("You're not allowed to write quotes!");
            return;
        } else if (Strings.isID(ctx.args.get(1))) {
            quoteCmdAddMessage(ctx);
        }

        String text = Strings.renderMessage(ctx.message, ctx.guild, ctx.rawArgs.substring(ctx.args.get(0).length()).trim()
                .replace('\n', ' '));

        if (text.length() > 360) {
            ctx.fail("Text too long!");
            return;
        }

        long quotes = dao.queryBuilder()
                .where()
                .eq("authorId", ctx.author.getIdLong())
                .countOf();

        if (quotes >= 25 && !Permissions.check(ctx, Permissions.BOT_ADMIN) &&
                ctx.author.getIdLong() != 219550353518100480L) {
            ctx.fail("You already have 25 quotes!");
            return;
        }

        Quote quote = new Quote(text,
                ctx.author.getIdLong(), ctx.author.getName());
        dao.create(quote);

        ctx.success("Quote added with ID `" + quote.getId() + "`.");
    }

    private void quoteCmdDelete(Context ctx) throws SQLException {
        if (ctx.args.length < 2) {
            ctx.fail("I need a quote ID to delete!");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(ctx.rawArgs.substring(ctx.args.get(0).length()).trim());
        } catch (NumberFormatException ignored) {
            ctx.fail("Invalid quote ID!");
            return;
        }

        Quote quote = dao.queryForId(id);
        if (quote == null) {
            ctx.fail("No such quote!");
            return;
        } else if (quote.getAuthorId() != ctx.author.getIdLong() &&
                ctx.author.getIdLong() != bot.owner.getIdLong() &&
                ctx.author.getIdLong() != quote.getQuotedById()) {
            ctx.fail("You didn't write or quote that quote!");
            return;
        }

        dao.deleteById(id);
        ctx.success("Quote deleted.");
    }

    private void quoteCmdList(Context ctx) throws SQLException {
        List<Quote> quotes = dao.queryBuilder()
                .orderBy("id", true)
                .query();

        if (quotes.size() < 1) {
            ctx.send("There are no quotes!").queue();
            return;
        }

        String[] renderedQuotes = new String[quotes.size()];
        for (int i = 0; i < quotes.size(); i++)
            renderedQuotes[i] = quotes.get(i).render();

        int page = 1;
        if (ctx.args.length > 1) {
            if (Strings.is4Digits(ctx.args.get(1))) {
                int wantedPage = Integer.parseInt(ctx.args.get(1));
                int max = (int) Math.ceil(renderedQuotes.length / 12);

                if (wantedPage > max) {
                    ctx.fail("No such page! There are **" + max + "** pages.");
                    return;
                } else {
                    page = wantedPage;
                }
            }
        }

        String name;
        Color color;
        if (ctx.guild == null) {
            color = Color.BLUE;
            name = ctx.author.getName();
        } else {
            color = val(ctx.member.getColor()).or(Color.RED);
            name = ctx.member.getEffectiveName();
        }

        Paginator.Builder builder = new Paginator.Builder()
                .setColumns(1)
                .useNumberedItems(false)
                .setItemsPerPage(12)
                .waitOnSinglePage(false)
                .showPageNumbers(true)
                .setColor(color)
                .setText("Listing all quotes:")
                .setItems(renderedQuotes)
                .setFinalAction(msg -> {
                    msg.editMessage(new MessageBuilder()
                            .append("Finished.")
                            .setEmbed(new EmbedBuilder()
                                    .setColor(color)
                                    .setAuthor(name, null, ctx.author.getEffectiveAvatarUrl())
                                    .setFooter("Full quote list", null)
                                    .build())
                            .build()).queue();

                    try {
                        msg.clearReactions().queue();
                    } catch (PermissionException|IllegalStateException ignored) {
                        //noinspection EmptyCatchBlock
                        try {
                            for (MessageReaction r: msg.getReactions()) {
                                r.removeReaction().queue();
                                r.removeReaction(ctx.author).queue();
                            }
                        } catch (PermissionException _ignored) {}
                    }
                })
                .setEventWaiter(bot.eventWaiter)
                .setTimeout(2, TimeUnit.MINUTES)
                .addUsers(ctx.author);

        builder.build().paginate(ctx.channel, page);
    }

    private void quoteCmdRandom(Context ctx) throws SQLException {
        Quote quote = dao.queryBuilder()
                .orderByRaw("RAND()")
                .limit(1L)
                .queryForFirst();

        ctx.send(quote.render()).queue();
    }

    private void quoteCmdCount(Context ctx) throws SQLException {
        ctx.send("There are **" + dao.countOf() + "** quotes.").queue();
    }

    private void quoteCmdAddMessage(Context ctx) throws SQLException {
        final int i = ctx._flag ? 0 : 1;
        if (ctx.args.length < 2 || !Strings.isID(ctx.args.get(i))) {
            ctx.fail("I need the ID of a message to quote!");
            return;
        } else if (banDao.queryForId(ctx.author.getIdLong()) != null) {
            ctx.fail("You're not allowed to quote anyone!");
            return;
        }

        Message msg;
        try {
            msg = ctx.channel.getMessageById(MiscUtil.parseSnowflake(ctx.args.get(i))).complete();
        } catch (ErrorResponseException ignored) {
            ctx.fail("No such message! (must be in this channel)");
            return;
        } catch (NumberFormatException ignored) {
            ctx.fail("Invalid message ID! Use Developer Mode to Copy ID.");
            return;
        }

        String text = Strings.renderMessage(msg, msg.getGuild(), msg.getContentRaw().replace('\n', ' '));

        if (text.length() > 360) {
            ctx.fail("Text too long!");
            return;
        } else if (text.length() < 8) {
            ctx.fail("That message needs to have valid text!");
            return;
        }

        long quotes = dao.queryBuilder()
                .where()
                .eq("authorId", msg.getAuthor().getIdLong())
                .countOf();

        if (quotes >= 25 && !Permissions.check(ctx, Permissions.BOT_ADMIN) &&
                msg.getAuthor().getIdLong() != 219550353518100480L) {
            ctx.fail("The author of that message already has 25 quotes, and you aren't an admin!");
            return;
        }

        Quote quote = new Quote(text,
                msg.getAuthor().getIdLong(), msg.getAuthor().getName());
        quote.setQuotedById(ctx.author.getIdLong());
        dao.create(quote);

        ctx.success("Quote added with ID `" + quote.getId() + "`.");
    }

    @Perm.Owner
    @Command(name = "quote_ban", desc = "Ban an user from adding quotes.", usage = "[@mention/user ID]",
            thread = true, aliases = {"quotes_ban", "qb"})
    public void cmdQuoteBan(Context ctx) throws SQLException {
        long userId;
        User user;

        if (ctx.message.getMentionedUsers().size() > 0) {
            user = ctx.message.getMentionedUsers().get(0);
            userId = user.getIdLong();
        } else if (Strings.isID(ctx.rawArgs)) {
            userId = MiscUtil.parseSnowflake(ctx.rawArgs);
            user = ctx.jda.retrieveUserById(userId).complete();
        } else {
            ctx.fail("You must @mention a user or provide their ID!");
            return;
        }

        int newN = banDao.create(new QuotesBannedMember(userId));
        DeleteBuilder builder = dao.deleteBuilder();
        builder.where()
                .eq("authorId", userId);
        int delN = builder.delete();

        if (newN > 0) {
            ctx.send(Emotes.getSuccess() + " Successfully banned **" + getTag(user) +
                    "** from adding quotes. All of their **" + delN + "** quotes have been deleted.").queue();
        } else {
            ctx.fail("**" + getTag(user) + "** was already banned from adding quotes.");
        }
    }

    @Perm.Owner
    @Command(name = "quote_ban_list", desc = "List users banned from adding quotes.", thread = true,
            aliases = {"quotes_ban_list", "qb_list"})
    public void cmdQuoteBanList(Context ctx) throws SQLException {
        String rendered =
                banDao.queryForAll().stream().map(q -> "**" +
                        getTag(ctx.jda.retrieveUserById(q.id).complete()) +
                "** (`" + q.id + "`)").collect(Collectors.joining("\n    \u2022 "));

        if (rendered.length() < 1) {
            ctx.success("Nobody is banned from adding quotes.");
        } else {
            ctx.send("The following users are banned from adding quotes:\n    \u2022 " + rendered).queue();
        }
    }

    @Perm.Owner
    @Command(name = "quote_ban_remove", desc = "Allow a banned user from adding quotes.", usage = "[@mention/user ID]",
            thread = true, aliases = {"quotes_ban_remove", "qb_remove", "qb_rm", "qb_del", "quote_ban_rm",
            "quote_ban_del", "quotes_ban_rm", "quotes_ban_del"})
    public void cmdQuoteBanRemove(Context ctx) throws SQLException {
        long userId;
        User user;

        if (ctx.message.getMentionedUsers().size() > 0) {
            user = ctx.message.getMentionedUsers().get(0);
            userId = user.getIdLong();
        } else if (Strings.isID(ctx.rawArgs)) {
            userId = MiscUtil.parseSnowflake(ctx.rawArgs);
            user = ctx.jda.retrieveUserById(userId).complete();
        } else {
            ctx.fail("You must @mention a user or provide their ID!");
            return;
        }

        int delN = banDao.deleteById(userId);

        if (delN > 0) {
            ctx.send(Emotes.getSuccess() + " Successfully unbanned **" + getTag(user) +
                    "** from adding quotes.").queue();
        } else {
            ctx.fail("**" + getTag(user) + "** isn't banned from adding quotes.");
        }
    }

    private void quoteShowId(Context ctx, int id) throws SQLException {
        Quote quote = dao.queryForId(id);

        if (quote == null)
            ctx.fail("No such quote!");
        else
            ctx.send(quote.render()).queue();
    }
}
