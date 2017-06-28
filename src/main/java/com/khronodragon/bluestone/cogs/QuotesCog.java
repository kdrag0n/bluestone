package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.sql.Quote;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;

public class QuotesCog extends Cog {
    private static final Logger logger = LogManager.getLogger(QuotesCog.class);
    private static final String NO_COMMAND = ":thinking: **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `[id]` - show quote `id`\n" +
            "    \u2022 `add [quote]` - add a quote\n" +
            "    \u2022 `delete [id]` - delete a quote, if you own it\n" +
            "    \u2022 `list` - list your quotes\n" +
            "    \u2022 `random` - view a random quote\n" +
            "    \u2022 `count` - see how many quotes there are\n" +
            "\n" +
            "Aliases: [create, new, remove], [del, rm], [rand], [num]";
    private Dao<Quote, Integer> dao;

    public QuotesCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), Quote.class);
        } catch (SQLException e) {
            logger.warn("Failed to create quote table!", e);
        }

        try {
            dao = DaoManager.createDao(bot.getShardUtil().getDatabase(), Quote.class);
        } catch (SQLException e) {
            logger.warn("Failed to create quote DAO!", e);
        }
    }

    public String getName() {
        return "Quotes";
    }

    public String getDescription() {
        return "Gotta quote 'em all!";
    }

    @Command(name = "quote", desc = "Add, create, or view a quote!", thread = true,
            usage = "[action / id] {args?...}", aliases = {"quotes"})
    public void cmdQuote(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 1) {
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
        else if (invoked.matches("^[0-9]{1,4}$"))
            quoteShowId(ctx, Integer.parseInt(invoked));
        else
            ctx.send(NO_COMMAND).queue();
    }

    private void quoteCmdAdd(Context ctx) throws SQLException {
        if (ctx.args.size() < 2) {
            ctx.send(":warning: I need text to quote!").queue();
            return;
        }
        String text = ctx.rawArgs.substring(ctx.args.get(0).length()).trim();

        if (text.length() > 360) {
            ctx.send(":warning: Text too long!").queue();
            return;
        }

        long quotes = dao.queryBuilder()
                .where()
                .eq("authorId", ctx.author.getIdLong())
                .countOf();

        if (quotes >= 25) {
            ctx.send(":x: You already have 25 quotes!").queue();
            return;
        }

        Quote quote = new Quote(text, ctx.author.getIdLong(), ctx.author.getName());
        dao.create(quote);

        ctx.send(":white_check_mark: Quote added with ID `" + quote.getId() + "`.").queue();
    }

    private void quoteCmdDelete(Context ctx) throws SQLException {
        if (ctx.args.size() < 2) {
            ctx.send(":warning: I need a quote ID to delete!").queue();
            return;
        }
        int id;
        try {
            id = Integer.parseInt(ctx.rawArgs.substring(ctx.args.get(0).length()).trim());
        } catch (NumberFormatException ignored) {
            ctx.send(":warning: Invalid quote ID!").queue();
            return;
        }

        Quote quote = dao.queryForId(id);
        if (quote == null) {
            ctx.send(":warning: No such quote!").queue();
            return;
        } else if (quote.getAuthorId() != ctx.author.getIdLong()) {
            ctx.send(":x: You didn't write that quote!").queue();
            return;
        }

        dao.deleteById(id);
        ctx.send(":white_check_mark: Quote deleted.").queue();
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

        PaginatorBuilder builder = new PaginatorBuilder()
                .setColumns(1)
                .useNumberedItems(false)
                .setItemsPerPage(12)
                .waitOnSinglePage(true)
                .showPageNumbers(true)
                .setText("Listing all quotes:")
                .setItems(renderedQuotes)
                .setFinalAction(msg -> {
                    msg.editMessage("Finished.").queue();

                    try {
                        msg.clearReactions().queue();
                    } catch (PermissionException ignored) {}
                })
                .setEventWaiter(new EventWaiter())
                .setTimeout(2, TimeUnit.MINUTES)
                .addUsers(ctx.author);

        if (ctx.guild == null)
            builder.setColor(randomColor());
        else
            builder.setColor(val(ctx.member.getColor()).or(randomColor()));

        builder.build().paginate(ctx.channel, 1);
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

    private void quoteShowId(Context ctx, int id) throws SQLException {
        Quote quote = dao.queryForId(id);

        if (quote == null)
            ctx.send(":warning: No such quote!").queue();
        else
            ctx.send(quote.render()).queue();
    }
}
