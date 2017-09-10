package com.khronodragon.bluestone.cogs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.PrivateChannel;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameDealCog extends Cog {
    private static final Logger logger = LogManager.getLogger(GameDealCog.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";
    private static final String HUMBLE_HOME = "https://www.humblebundle.com/";
    private static final String STEAM_FEATURED = "http://store.steampowered.com/api/featured/";
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[Image]\\([a-zA-Z/0-9?=]+\\)");
    private static final Pattern MULTI_NEWLINE_PATTERN = Pattern.compile("\n{3,}");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#+)\\s*([^#]+)\\s*\\1?", Pattern.MULTILINE);
    private static final List<Deal> emptyDealList = new ArrayList<>(0);
    private static final Remark remark;
    private static volatile int scheduledShardNum = -1;
    private static final ScheduledExecutorService scheduledExec = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("GameDeal Checker Thread %d")
            .build());
    private static AtomicBoolean hasScheduled = new AtomicBoolean(false);

    static {
        Options options = Options.markdown();
        options.autoLinks = true;
        options.setReverseAllSmarts(true);
        options.hardwraps = true;
        options.fencedCodeBlocks = Options.FencedCodeBlocks.ENABLED_BACKTICK;
        options.tables = Options.Tables.CONVERT_TO_CODE_BLOCK;
        options.inWordEmphasis = Options.InWordEmphasis.NORMAL;
        options.inlineLinks = true;

        remark = new Remark(options);
    }

    public GameDealCog(Bot bot) {
        super(bot);

        if (!hasScheduled.getAndSet(true)) {
            schedule();
            scheduledShardNum = bot.getShardNum();
        }
    }

    private void schedule() {
        Set<Deal> deals = new HashSet<>();

        scheduledExec.scheduleAtFixedRate(() -> {
            try {
                List<Deal> steamDeals = checkSteam();
                if (steamDeals == null)
                    steamDeals = emptyDealList;

                List<Deal> humbleDeals = checkHumbleBundle();
                if (humbleDeals == null)
                    humbleDeals = emptyDealList;
            } catch (Exception e) {
                logger.error("Error in scheduled GameDeal checker", e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    public String getName() {
        return "GameDeal";
    }

    public String getDescription() {
        return "Never miss the best free deals on Steam and Humble Bundle again!";
    }

    public void unload() {
        super.unload();

        if (bot.getShardNum() == scheduledShardNum) {
            scheduledShardNum = -1;
            hasScheduled.set(false);
        }
    }

    private static List<Deal> checkSteam() {
        String rawResponse;
        try {
            rawResponse = webFetch(STEAM_FEATURED);
        } catch (IOException e) {
            logger.error("Error fetching Steam Storefront deals", e);
            return null;
        }

        JSONObject resp = new JSONObject(rawResponse);
        List<Deal> deals = new LinkedList<>();

        for (String rootKey: resp.keySet()) {
            JSONArray apps = resp.optJSONArray(rootKey);
            if (apps != null && apps.length() > 0 && apps.optJSONObject(0) != null &&
                    apps.getJSONObject(0).optString("name", null) != null) {
                for (int i = 0; i < apps.length(); i++) {
                    JSONObject app = apps.getJSONObject(i);
                    if (!app.optBoolean("discounted", false))
                        continue;

                    short discountPercent = (short)app.optInt("discount_percent", 0);
                    if (discountPercent < 25)
                        continue;

                    int appID = app.optInt("id", 0);
                    String desc = "¯\\_(ツ)_/¯";
                    try {
                        String url = "http://store.steampowered.com/api/appdetails/?appids=" + appID;
                        desc = new JSONObject(Bot.http.newCall(new Request.Builder()
                                .get()
                                .url(url)
                                .build()).execute().body().string())
                                .getJSONObject(Integer.toUnsignedString(appID))
                                .getJSONObject("data").getString("about_the_game");
                        Document descDoc = Jsoup.parse(desc, url);
                        desc = remark.convert(descDoc);
                    } catch (Exception e) {
                        logger.error("Error fetching Steam app description", e);
                    }

                    int iPrice = app.optInt("final_price", 0);
                    boolean free = iPrice < 1;

                    Deal deal = new Deal(DealSource.STEAM, free, app.optString("name", "None?!"),
                            desc.toString(),
                            "http://store.steampowered.com/app/" + appID,
                            app.optString("large_capsule_image", null), ((float)iPrice) / 100.f,
                            discountPercent);
                    deals.add(deal);
                }
            }
        }

        return deals;
    }

    private static String webFetch(String url) throws IOException {
        return Bot.http.newCall(new Request.Builder()
                .get()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()).execute().body().string();
    }

    private List<Deal> checkHumbleBundle() {
        List<Deal> deals = new LinkedList<>();

        String rawResponse;
        try {
            rawResponse = webFetch(HUMBLE_HOME);
        } catch (IOException e) {
            logger.error("Error fetching Humble Bundle home", e);
            return null;
        }

        Document home = Jsoup.parse(rawResponse, HUMBLE_HOME);
        List<Deal> homeDeals = humbleFindDeals(home);
        if (homeDeals != null)
            deals.addAll(homeDeals);

        Elements otherBundleLinks = home.getElementById("subtab-container").getElementsByClass("subtab-button");
        for (Element link: otherBundleLinks) {
            if (link.classNames().contains("active"))
                continue;

            String url = link.attr("abs:href");
            try {
                List<Deal> pageDeals = humbleFindDeals(Jsoup.parse(webFetch(url), url));

                if (pageDeals != null)
                    deals.addAll(pageDeals);
            } catch (Exception e) {
                logger.error("Error fetching Humble Bundle linked page {}", url, e);
            }
        }

        return deals;
    }

    private List<Deal> humbleFindDeals(Document page) {
        List<Deal> deals = new LinkedList<>();

        for (Element row: page.getElementsByClass("fi-row")) {
            Element block = row.getElementsByClass("fi-content").first();
            Element header = row.getElementsByClass("fi-content").first();

            Element body = block.getElementsByClass("fi-content-body").first();
            Element title = body.getElementsByTag("p").first();
            Element desc = body.getElementsByTag("span").first();
            Element image = row.getElementsByClass("fi-image").first().getElementsByTag("img").first();

            deals.add(new Deal(DealSource.HUMBLE_BUNDLE, header.text().contains("FREE"), title.text(),
                    remark.convertFragment(desc.html(), page.baseUri()),
                    page.baseUri(), image.absUrl("data-retina-src"), 0.f, (short)100));
        }

        if (deals.isEmpty())
            return null;
        else
            return deals;
    }

    private static MessageEmbed renderDeal(Deal deal) {
        return new EmbedBuilder()
                .setAuthor(deal.free ? "New FREE" : "New" + " deal on " + deal.source.name + '!', deal.link, deal.source.icon)
                .setTitle(deal.title)
                .addField("Price", deal.free ? "**$0.00**!" : "$" + deal.price + " (" + deal.discountPercent + "% off)", false)
                .setDescription(filterDescription(deal.description))
                .setImage(deal.imageURL)
                .setTimestamp(Instant.now())
                .setColor(randomColor())
                .build();
    }

    private static String filterDescription(String desc) { // TODO: fix this, not doing anything
        Matcher matcher = IMAGE_PATTERN.matcher(desc);
        matcher.replaceAll("");
        matcher.usePattern(HEADER_PATTERN).replaceAll("**__$2__**");
        desc = matcher.usePattern(MULTI_NEWLINE_PATTERN).replaceAll("\n\n");
        return desc;
    }

    @Command(name = "gdtest", desc = "Test GameDeal providers by sending all deals found.", thread = true)
    public void cmdGdTest(Context ctx) {
        ctx.send("Checking...").queue();

        for (Deal deal: checkSteam())
            ctx.send(renderDeal(deal)).queue();
        for (Deal deal: checkHumbleBundle())
            ctx.send(renderDeal(deal)).queue();

        ctx.send("Done.").queue();
    }

    @Command(name = "gamedeal", desc = "Manage your GameDeal settings.")
    public void cmdGameDeal(Context ctx) {
        if (ctx.channel instanceof PrivateChannel) {

        } else {

        }
    }

    private static class Deal {
        private DealSource source;
        private boolean free;
        private String title;
        private String description;
        private String link;
        private String imageURL;
        private float price;
        private short discountPercent;

        private Deal(DealSource source, boolean free, String title, String description, String link, String imageURL, float price, short discountPercent) {
            this.source = source;
            this.free = free;
            this.title = title;
            this.description = description;
            this.link = link;
            this.imageURL = imageURL;
            this.price = price;
            this.discountPercent = discountPercent;
        }
    }

    private enum DealSource {
        STEAM("Steam", "http://www.freeiconspng.com/uploads/steam-logo-icon-7.png"),
        HUMBLE_BUNDLE("Humble Bundle", "https://humblebundle-a.akamaihd.net/static/hashed/46cf2ed85a0641bfdc052121786440c70da77d75.png"),
        ORIGIN("Origin", "https://seeklogo.com/images/O/origin-logo-BF01A5BFBA-seeklogo.com.png"),
        REDDIT("Reddit", "https://www.redditstatic.com/icon.png");

        private String name;
        private String icon;

        DealSource(String name, String icon) {
            this.name = name;
            this.icon = icon;
        }
    }
}
