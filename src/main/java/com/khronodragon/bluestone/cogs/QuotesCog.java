package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.sql.Quote;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.ObjectName;
import java.awt.*;
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

        //ObjectName objectName = new ObjectName("com.khronodragon.bluestone.cogs:type=QuotesCog");
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
            ctx.send(Emotes.getFailure() + " I need text to quote!").queue();
            return;
        }
        String text = ctx.rawArgs.substring(ctx.args.get(0).length()).trim();

        if (text.length() > 360) {
            ctx.send(Emotes.getFailure() + " Text too long!").queue();
            return;
        }

        long quotes = dao.queryBuilder()
                .where()
                .eq("authorId", ctx.author.getIdLong())
                .countOf();

        if (quotes >= 25) {
            ctx.send(Emotes.getFailure() + " You already have 25 quotes!").queue();
            return;
        }

        Quote quote = new Quote(text.replace('\n', ' '),
                ctx.author.getIdLong(), ctx.author.getName());
        dao.create(quote);

        ctx.send(Emotes.getSuccess() + " Quote added with ID `" + quote.getId() + "`.").queue();
    }

    private void quoteCmdDelete(Context ctx) throws SQLException {
        if (ctx.args.size() < 2) {
            ctx.send(Emotes.getFailure() + " I need a quote ID to delete!").queue();
            return;
        }
        int id;
        try {
            id = Integer.parseInt(ctx.rawArgs.substring(ctx.args.get(0).length()).trim());
        } catch (NumberFormatException ignored) {
            ctx.send(Emotes.getFailure() + " Invalid quote ID!").queue();
            return;
        }

        Quote quote = dao.queryForId(id);
        if (quote == null) {
            ctx.send(Emotes.getFailure() + " No such quote!").queue();
            return;
        } else if (quote.getAuthorId() != ctx.author.getIdLong() &&
                ctx.author.getIdLong() != bot.owner.getIdLong()) {
            ctx.send(Emotes.getFailure() + " You didn't write that quote!").queue();
            return;
        }

        dao.deleteById(id);
        ctx.send(Emotes.getSuccess() + " Quote deleted.").queue();
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
        if (ctx.args.size() > 1) {
            if (ctx.args.get(1).matches("^[0-9]{1,3}$")) {
                int wantedPage = Integer.parseInt(ctx.args.get(1));
                int max = (int) Math.ceil(renderedQuotes.length / 12);

                if (wantedPage > max) {
                    ctx.send(Emotes.getFailure() + " No such page! There are **" + max + "** pages.").queue();
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

        PaginatorBuilder builder = new PaginatorBuilder()
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
                    } catch (PermissionException ignored) {}
                })
                .setEventWaiter(bot.getEventWaiter())
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

    private void quoteShowId(Context ctx, int id) throws SQLException {
        Quote quote = dao.queryForId(id);

        if (quote == null)
            ctx.send(Emotes.getFailure() + " No such quote!").queue();
        else
            ctx.send(quote.render()).queue();
    }
}
