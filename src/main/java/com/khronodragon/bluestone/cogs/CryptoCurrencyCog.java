package com.khronodragon.bluestone.cogs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.exceptions.PermissionException;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.str;
import static com.khronodragon.bluestone.util.Strings.format;

public class CryptoCurrencyCog extends Cog {
    private static final Logger logger = LogManager.getLogger(CryptoCurrencyCog.class);
    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    private static volatile Map<String, Cryptocurrency> currencies = new LinkedHashMap<>();
    private static final ScheduledExecutorService scheduledExec = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Cryptocurrency Updater Thread %d")
            .build());
    private static volatile List<String> rankedList = new LinkedList<>();
    private static Field stringsField;

    static {
        scheduledExec.scheduleAtFixedRate(CryptoCurrencyCog::update, 0, 15, TimeUnit.MINUTES);
        try {
            stringsField = PaginatorBuilder.class.getDeclaredField("strings");
            stringsField.setAccessible(true);
        } catch (Exception e) {
            logger.error("Error getting strings field of PaginatorBuilder", e);
            stringsField = null;
        }
    }

    public CryptoCurrencyCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Cryptocurrencies";
    }

    public String getDescription() {
        return "Exchange, view, and sort various cryptocurrencies. Over 1000 are supported!";
    }

    private static void update() {
        try {
            Cryptocurrency[] data;
            try (ResponseBody rbody = Bot.http.newCall(new Request.Builder()
                    .get()
                    .url("https://api.coinmarketcap.com/v1/ticker/?convert=EUR")
                    .build()).execute().body()) {
                data = gson.fromJson(rbody.charStream(), Cryptocurrency[].class);
            }

            Map<String, Cryptocurrency> newCurrencies = new LinkedHashMap<>();
            List<String> newRanked = new LinkedList<>();

            for (Cryptocurrency currency: data) {
                currency.updateTime = Instant.ofEpochMilli(currency.lastUpdated);
                newCurrencies.put(currency.symbol, currency);

                newRanked.add(format("{0,number}. **{1}** ({2}) - **${3,number}**, {4,number} available",
                        currency.rank, currency.name, currency.symbol, currency.priceUSD, currency.availableSupply));
                // TODO: revise format and details
            }

            currencies = newCurrencies;
            rankedList = newRanked;
        } catch (Exception e) {
            logger.error("Error updating cryptocurrency data", e);
        }
    }

    @Command(name = "convert", desc = "Convert between different currencies.", aliases = {"exchange"},
            usage = "[from currency] [to currency] [amount]")
    public void cmdConvert(Context ctx) {
        Cryptocurrency from;
        double amount;

        if (ctx.args.size() != 3) {
            ctx.send(Emotes.getFailure() + " Usage is `[from currency] [to currency] [amount]`!").queue();
            return;
        } else if ((from = currencies.get(ctx.args.get(0))) == null) {
            ctx.send(Emotes.getFailure() + " No such cryptocurrency `" + ctx.args.get(0) + "`!").queue();
            return;
        }

        String to = ctx.args.get(1);

        try {
            amount = Double.parseDouble(ctx.args.get(2));
        } catch (NumberFormatException ignored) {
            ctx.send(Emotes.getFailure() + " Invalid amount!").queue();
            return;
        }
        if (amount <= 0.d) {
            ctx.send(Emotes.getFailure() + " Invalid amount!").queue();
            return;
        }

        double converted;
        if (to.equals("USD")) {
            converted = from.priceUSD * amount;
        } else if (to.equals("EUR")) {
            converted = from.priceEUR * amount;
        } else if (to.equals("BTC")) {
            converted = from.priceBTC * amount;
        } else {
            Cryptocurrency toCurrency;
            if ((toCurrency = currencies.get(to)) == null) {
                ctx.send(Emotes.getFailure() + " No such cryptocurrency `" + to + "`!").queue();
                return;
            }

            converted = (from.priceBTC * amount) / toCurrency.priceBTC;
        }

        ctx.send(format("{0,number} {1} = **{2,number} {3}**", amount, from.symbol, converted, to)).queue();
    }

    @Command(name = "currencyinfo", desc = "Get detailed information about a cryptocurrency.",
            aliases = {"currency", "currency_info"}, usage = "[cryptocurrency symbol]")
    public void cmdCurrencyInfo(Context ctx) {
        Cryptocurrency c;

        if (ctx.args.size() < 1) {
            ctx.send(Emotes.getFailure() + " I need a cryptocurrency to give information on!").queue();
            return;
        } else if ((c = currencies.get(ctx.args.get(0))) == null) {
            ctx.send(Emotes.getFailure() + " No such cryptocurrency `" + ctx.args.get(0) + "`!").queue();
            return;
        }

        String iconUrl = "https://files.coinmarketcap.com/static/img/coins/128x128/" + c.id + ".png";

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setTimestamp(c.updateTime)
                .setFooter("Updated at", null)
                .setAuthor(c.name + '(' + c.symbol + ')',
                        "https://coinmarketcap.com/currencies/" + c.id + '/', iconUrl)
                .setThumbnail(iconUrl)
                .addField("Rank", '#' + str(c.rank), false)
                .addField("Price", format("USD ${0,number}\nBTC Ƀ{1,number}\nEUR €{2,number}",
                        c.priceUSD, c.priceBTC, c.priceEUR), false)
                .addField("Available Supply", format("{0,number}", c.availableSupply), true)
                .addField("Total Supply", format("{0,number}", c.totalSupply), true)
                .addField("Market Cap", format("USD ${0,number}\nEUR €{1,number}",
                        c.marketCapUSD, c.marketCapEUR), false)
                .addField("% Changed in Last Hour", format("{0,number}%", c.percentChange1h), true)
                .addField("% Changed in Last Day", format("{0,number}%", c.percentChange24h), true)
                .addField("% Changed in Last Week", format("{0,number}%", c.percentChange7d), true)
                .addField("Volume (last day)", format("USD ${0,number}\nEUR €{1,number}",
                        c.volume24hUSD, c.volume24hEUR), false)
                .build()).queue();
    }

    @Command(name = "currencies", desc = "List all cryptocurrencies (paginated), sorted by value.",
            aliases = {"cryptolist", "cryptocurrencies", "currencylist", "currency_list"})
    public void cmdCurrencyList(Context ctx) throws IllegalAccessException {
        String name;
        Color color;
        if (ctx.guild == null) {
            color = Color.BLUE;
            name = ctx.author.getName();
        } else {
            color = val(ctx.member.getColor()).or(Color.RED);
            name = ctx.member.getEffectiveName();
        }

        int page = 1;
        if (ctx.args.size() > 1) {
            if (Strings.is4Digits(ctx.args.get(1))) {
                int wantedPage = Integer.parseInt(ctx.args.get(1));
                int max = (int) Math.ceil(rankedList.size() / 12);

                if (wantedPage > max) {
                    ctx.send(Emotes.getFailure() + " No such page! There are **" + max + "** pages.").queue();
                    return;
                } else {
                    page = wantedPage;
                }
            }
        }

        PaginatorBuilder builder = new PaginatorBuilder()
                .setColumns(1)
                .useNumberedItems(false)
                .setItemsPerPage(12)
                .waitOnSinglePage(false)
                .showPageNumbers(true)
                .setColor(color)
                .setText("Listing all cryptocurrencies.")
                .setFinalAction(msg -> {
                    msg.editMessage(new MessageBuilder()
                            .append("Finished.")
                            .setEmbed(new EmbedBuilder()
                                    .setColor(color)
                                    .setAuthor(name, null, ctx.author.getEffectiveAvatarUrl())
                                    .setFooter("Cryptocurrency list", null)
                                    .build())
                            .build()).queue();

                    try {
                        msg.clearReactions().queue();
                    } catch (PermissionException ignored) {}
                })
                .setEventWaiter(bot.getEventWaiter())
                .setTimeout(2, TimeUnit.MINUTES)
                .addUsers(ctx.author);

        stringsField.set(builder, rankedList);

        builder.build().paginate(ctx.channel, page);
        // TODO:  set timestamp on embed
    }

    private static class Cryptocurrency {
        public String id;
        public String name;
        public String symbol;
        public short rank;
        @SerializedName("price_usd")
        public double priceUSD;
        @SerializedName("price_btc")
        public double priceBTC;
        @SerializedName("price_eur")
        public double priceEUR;
        @SerializedName("24h_volume_usd")
        public double volume24hUSD;
        @SerializedName("24h_volume_eur")
        public double volume24hEUR;
        @SerializedName("market_cap_usd")
        public double marketCapUSD;
        @SerializedName("market_cap_eur")
        public double marketCapEUR;
        public double availableSupply;
        public double totalSupply;
        @SerializedName("percent_change_1h")
        public float percentChange1h;
        @SerializedName("percent_change_24h")
        public float percentChange24h;
        @SerializedName("percent_change_7d")
        public float percentChange7d;
        public long lastUpdated;
        public Instant updateTime;
    }
}
