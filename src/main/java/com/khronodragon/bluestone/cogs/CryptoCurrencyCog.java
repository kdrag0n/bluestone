package com.khronodragon.bluestone.cogs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    private static volatile Cryptocurrency[] currencyArray = new Cryptocurrency[]{};
    private final NumberFormat dec = DecimalFormat.getInstance();
    private static volatile Instant lastUpdated = Instant.ofEpochMilli(0);
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
                Instant now = Instant.now();

                if (currency.updateTime.isBefore(now.minus(Duration.ofDays(1)))) {
                    currency.updateTime = now;
                }

                newCurrencies.put(currency.symbol, currency);

                String pcKey = currency.percentChange24h > 0.d ? "+" : "";

                newRanked.add(format("{0,number}. **{1}** ({2}) \u2022 **${3,number}** \u2022 {4}{5,number}% changed in last 24h",
                        currency.rank, currency.name, currency.symbol, currency.priceUSD, pcKey, currency.percentChange24h));
            }

            currencies = newCurrencies;
            lastUpdated = Instant.now();
            rankedList = newRanked;
            currencyArray = currencies.values().toArray(new Cryptocurrency[0]);
        } catch (Exception e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                logger.error("Timeout updating cryptocurrency data");
            } else {
                logger.error("Error updating cryptocurrency data", e);
            }
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

    private EmbedBuilder renderDetails(Cryptocurrency c) {
        String iconUrl = "https://files.coinmarketcap.com/static/img/coins/128x128/" + c.id + ".png";

        return new EmbedBuilder()
                .setColor(randomColor())
                .setTimestamp(c.updateTime)
                .setFooter("Updated at", null)
                .setAuthor(c.name + '(' + c.symbol + ')',
                        "https://coinmarketcap.com/currencies/" + c.id + '/', iconUrl)
                .setThumbnail(iconUrl)
                .addField("Rank", '#' + str(c.rank), false)
                .addField("Price", format("USD ${0,number}\nBTC Ƀ{1,number}\nEUR €{2,number}",
                        c.priceUSD, c.priceBTC, c.priceEUR), false)
                .addField("Available Supply", dec.format(c.availableSupply), true)
                .addField("Total Supply", dec.format(c.totalSupply), true)
                .addField("Market Cap", format("USD ${0,number}\nEUR €{1,number}",
                        c.marketCapUSD, c.marketCapEUR), false)
                .addField("% Changed in Last Hour", format("{0,number}%", c.percentChange1h), true)
                .addField("% Changed in Last Day", format("{0,number}%", c.percentChange24h), true)
                .addField("% Changed in Last Week", format("{0,number}%", c.percentChange7d), true)
                .addField("Volume (last day)", format("USD ${0,number}\nEUR €{1,number}",
                        c.volume24hUSD, c.volume24hEUR), false);
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

        ctx.send(renderDetails(c).build()).queue();
    }

    private MessageEmbed dclRenderPage(int i) {
        return renderDetails(currencyArray[i])
                .addField("\u200b", "Page " + (i + 1) + " of " + currencyArray.length, false)
                .build();
    }

    private void dclStep(long authorId, AtomicInteger index, Message msg, Runnable stop) {
        bot.getEventWaiter().waitForEvent(MessageReactionAddEvent.class, e -> {
            String emote = e.getReactionEmote().getName();
            return e.getMessageIdLong() == msg.getIdLong() &&
                    (emote.equals("◀") || emote.equals("⏹") || emote.equals("▶")) &&
                    e.getUser().getIdLong() == authorId;
        }, e -> {
            String emote = e.getReactionEmote().getName();

            if (emote.equals("◀")) {
                if (index.get() > 0)
                    index.decrementAndGet();
            } else if (emote.equals("⏹")) {
                stop.run();
            } else if (emote.equals("▶")) {
                if (index.get() < currencyArray.length - 1)
                    index.incrementAndGet();
            } else {
                stop.run();
            }

            msg.editMessage(dclRenderPage(index.get())).queue();
            try {
                e.getReaction().removeReaction(e.getUser()).queue(null, ignored -> {});
            } catch (Exception ignored) {}

            dclStep(authorId, index, msg, stop);
        }, 2, TimeUnit.MINUTES, stop);
    }

    @Command(name = "dcl", desc = "List all cryptocurrencies, with a detailed info page for each one.",
            usage = "{page #}")
    public void cmdDclFull(Context ctx) {
        AtomicInteger index = new AtomicInteger(0);
        Consumer<Throwable> failure = ignored -> {
            ctx.send(Emotes.getFailure() + " Error setting up list!").queue();
        };

        ctx.send(dclRenderPage(0)).queue(msg -> {
            msg.addReaction("◀").queue(v1 -> {
                msg.addReaction("⏹").queue(v2 -> {
                    msg.addReaction("▶").queue(v3 -> {
                        Runnable stop = () -> {
                            try {
                                msg.clearReactions().queue();
                            } catch (PermissionException|IllegalStateException ignored) {
                                try {
                                    for (MessageReaction r: msg.getReactions()) {
                                        r.removeReaction().queue();
                                    }
                                } catch (PermissionException i) {}
                            }
                        };

                        dclStep(ctx.author.getIdLong(), index, msg, stop);
                    }, failure);
                }, failure);
            }, failure);
        }, failure);
    }

    @Command(name = "currencies", desc = "List all cryptocurrencies (paginated), sorted by market cap.",
            aliases = {"cryptolist", "cryptocurrencies", "currencylist", "currency_list"}, usage = "{page #}")
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
        if (ctx.args.size() > 0) {
            if (Strings.is4Digits(ctx.args.get(0))) {
                int wantedPage = Integer.parseInt(ctx.args.get(0));
                int max = (int) Math.ceil(rankedList.size() / 16);

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
                .setItemsPerPage(16)
                .waitOnSinglePage(false)
                .showPageNumbers(true)
                .setColor(color)
                .setText("Listing all cryptocurrencies:")
                .setFinalAction(msg -> {
                    msg.editMessage(new MessageBuilder()
                            .append("Finished.")
                            .setEmbed(new EmbedBuilder()
                                    .setColor(color)
                                    .setAuthor(name, null, ctx.author.getEffectiveAvatarUrl())
                                    .setFooter("Data updated at", null)
                                    .setTimestamp(lastUpdated)
                                    .build())
                            .build()).queue();

                    try {
                        msg.clearReactions().queue();
                    } catch (PermissionException|IllegalStateException ignored) {
                        try {
                            for (MessageReaction r: msg.getReactions()) {
                                r.removeReaction().queue();
                                r.removeReaction(ctx.author).queue();
                            }
                        } catch (PermissionException i) {}
                    }
                })
                .setEventWaiter(bot.getEventWaiter())
                .setTimeout(2, TimeUnit.MINUTES)
                .addUsers(ctx.author);

        stringsField.set(builder, rankedList);

        builder.build().paginate(ctx.channel, page);
    }

    @Perm.Owner
    @Perm.Admin
    @Command(name = "cryptoupdate", desc = "Update the cryptocurrency data.",
            thread = true, aliases = {"currencyupdate", "cupdate", "crypto_update"})
    public void cmdCryptoUpdate(Context ctx) {
        update();
        ctx.send(Emotes.getSuccess() + " Updated cryptocurrency data.").queue();
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
