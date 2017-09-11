package com.khronodragon.bluestone.cogs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;

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

    static {
        scheduledExec.scheduleAtFixedRate(CryptoCurrencyCog::update, 0, 15, TimeUnit.MINUTES);
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
            for (Cryptocurrency currency: data) {
                newCurrencies.put(currency.symbol, currency);
            }
            currencies = newCurrencies;
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
    }
}
