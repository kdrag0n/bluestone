package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.sql.Quote;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;

public class QuotesCog extends Cog {
    private static final Logger logger = LogManager.getLogger(QuotesCog.class);
    private static final Character[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String NO_COMMAND = ":thinking: **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `[id]` - show quote `id`\n" +
            "    \u2022 `add [quote]` - add a quote\n" +
            "    \u2022 `delete [id]` - delete a quote, if you own it\n" +
            "    \u2022 `list` - list your quotes\n" +
            "    \u2022 `random` - view a random quote";
    private Dao<Quote, String> dao;

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

    private char genChar() {
        return randomChoice(hexChars);
    }

    private String generateUniqueId() throws SQLException {
        boolean satisfied = false;
        String id = "0001";

        while (!satisfied) {
            StringBuilder idBuilder = new StringBuilder();
            for (short i = 0; i < 4; i++)
                idBuilder.append(genChar());
            id = idBuilder.toString();

            if (!dao.idExists(id))
                satisfied = true;
        }

        return id;
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
        else if (invoked.matches("^[0-9a-fA-F]{4}$"))
            quoteShowId(ctx, invoked.toLowerCase());
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

        int quotes = dao.queryForFieldValues(Collections.singletonMap("authorId", ctx.author.getIdLong())).size();
        if (quotes >= 25) {
            ctx.send(":x: You already have 25 quotes!").queue();
            return;
        }

        String id = generateUniqueId();
        Quote quote = new Quote(id, text, ctx.author.getIdLong(), ctx.author.getName());
        dao.createOrUpdate(quote);

        ctx.send(":white_check_mark: Quote added with ID `" + id + "`.").queue();
    }

    private void quoteCmdDelete(Context ctx) throws SQLException {
        if (ctx.args.size() < 2) {
            ctx.send(":warning: I need a quote ID to delete!").queue();
            return;
        }
        String id = ctx.rawArgs.substring(ctx.args.get(0).length()).trim();

        if (!dao.idExists(id)) {
            ctx.send(":warning: No such quote!").queue();
            return;
        }

        dao.deleteById(id);
        ctx.send(":white_check_mark: Quote deleted.").queue();
    }

    private void quoteCmdList(Context ctx) throws SQLException {
        List<Quote> quotes = dao.queryForFieldValues(Collections.singletonMap("authorId", ctx.author.getIdLong()));
        if (quotes.size() < 1) {
            ctx.send("You have no quotes. Add some!").queue();
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor(ctx.author.getName(), null, ctx.author.getEffectiveAvatarUrl())
                .setTitle("Quotes")
                .setDescription("Here are all the quotes you've written.");
        if (ctx.guild == null)
            emb.setColor(randomColor());
        else
            emb.setColor(val(ctx.member.getColor()).or(randomColor()));

        for (Quote quote: quotes) {
            emb.addField(quote.getId(), quote.getQuote(), true);
        }

        ctx.send(emb.build()).queue();
    }

    private void quoteCmdRandom(Context ctx) throws SQLException {
        List<Quote> quotes = dao.queryForAll();

        ctx.send(randomChoice(quotes).render()).queue();
    }

    private void quoteShowId(Context ctx, String id) throws SQLException {
        Quote quote = dao.queryForId(id);

        if (quote == null)
            ctx.send(":warning: No such quote!").queue();
        else
            ctx.send(quote.render()).queue();
    }
}
