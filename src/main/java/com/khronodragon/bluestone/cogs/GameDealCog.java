package com.khronodragon.bluestone.cogs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.sql.GameDealDestination;
import com.khronodragon.bluestone.util.GraphicsUtils;
import com.khronodragon.bluestone.util.Strings;
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.PermissionException;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
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
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[Image]\\([a-zA-Z/0-9?=:.]+\\)");
    private static final Pattern MULTI_NEWLINE_PATTERN = Pattern.compile("\\R{3,}");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#+)\\s*([^#]+)\\s*\\1?", Pattern.MULTILINE);
    private static final Color ECOLOR_FROM = new Color(218, 79, 66);
    private static final Color ECOLOR_TO = new Color(241, 167, 52);
    private static final Remark remark;
    private static volatile List<Deal> lastCheckResult = new ArrayList<>(0);
    private static volatile int scheduledShardNum = -1;
    private static final ScheduledExecutorService scheduledExec = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("GameDeal Checker Thread %d")
            .build());
    private static final LoadingCache<Integer, String> gameDescriptionCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.HOURS)
            .initialCapacity(24)
            .maximumSize(64)
            .build(new CacheLoader<Integer, String>() {
                @Override
                public String load(Integer appID) throws Exception {
                    String url = "http://store.steampowered.com/api/appdetails/?appids=" + appID;
                    return new JSONObject(Bot.http.newCall(new Request.Builder()
                            .get()
                            .url(url)
                            .build()).execute().body().string())
                            .getJSONObject(appID.toString())
                            .getJSONObject("data").getString("about_the_game");
                }
            });
    private static AtomicBoolean hasScheduled = new AtomicBoolean(false);
    private static final String NO_COMMAND = "🤔 **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `subscribe [#channel]` - set the GameDeal channel for this server\n" +
            "    \u2022 `min [1-100]` - set the minimum discount percent (default: 50%)\n" +
            "    \u2022 `steam` - toggle Steam deals\n" +
            "    \u2022 `humble` - toggle Humble Bundle deals\n" +
            "    \u2022 `disable` - disable GameDeal for this server or user (DM)\n" +
            "\n" +
            "You can also sign up for GameDeal in DMs, so the bot will DM you deals!\n" +
            "Just do `!gamedeal subscribe` without arguments, in a DM.\n" +
            "You will only receive new deals, not deals started before your subscription.";
    private Dao<GameDealDestination, Long> dao;
    private Dao<BroadcastedDeal, Integer> dealDao;

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

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), GameDealDestination.class);
        } catch (SQLException e) {
            logger.warn("Failed to create game deal table!", e);
        }

        try {
            dao = DaoManager.createDao(bot.getShardUtil().getDatabase(), GameDealDestination.class);
        } catch (SQLException e) {
            logger.warn("Failed to create game deal DAO!", e);
        }

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), BroadcastedDeal.class);
        } catch (SQLException e) {
            logger.warn("Failed to create broadcasted game deal table!", e);
        }

        try {
            dealDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), BroadcastedDeal.class);
        } catch (SQLException e) {
            logger.warn("Failed to create broadcasted game deal DAO!", e);
        }

        if (!hasScheduled.getAndSet(true)) {
            schedule();
            scheduledShardNum = bot.getShardNum();
        }
    }

    private void schedule() {
        scheduledExec.scheduleAtFixedRate(() -> {
            try {
                List<Deal> dealsNow = new LinkedList<>();
                List<Deal> steamDeals = checkSteam();
                if (steamDeals != null)
                    dealsNow.addAll(steamDeals);

                List<Deal> humbleDeals = checkHumbleBundle();
                if (humbleDeals != null)
                    dealsNow.addAll(humbleDeals);

                for (Deal deal: dealsNow) {
                    int dHash = deal.hashCode();

                    if (!dealDao.idExists(dHash)) {
                        try {
                            broadcastDeal(deal);
                        } catch (Exception e) {
                            logger.error("Failed to broadcast deal", e);
                        }
                        dealDao.create(new BroadcastedDeal(dHash));
                    }
                }

                lastCheckResult = dealsNow;
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
                    int appID = app.optInt("id", 0);
                    String desc = "¯\\_(ツ)_/¯";
                    try {
                        desc = gameDescriptionCache.get(appID);
                        Document descDoc = Jsoup.parse(desc);
                        desc = remark.convert(descDoc);
                    } catch (Exception e) {
                        logger.error("Error fetching Steam app description", e);
                    }

                    int iPrice = app.optInt("final_price", 0);
                    boolean free = iPrice < 1;

                    Deal deal = new Deal(DealSource.STEAM, free, app.optString("name", "None?!"),
                            desc, "http://store.steampowered.com/app/" + appID,
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
                .setColor(GraphicsUtils.interpolateColors(ECOLOR_FROM, ECOLOR_TO, (double)deal.discountPercent / 100.d))
                .build();
    }

    private static boolean shouldSend(GameDealDestination dest, Deal deal) {
        switch (deal.source) {
            case STEAM:
                if (!dest.isSteam()) return false;
                break;
            case HUMBLE_BUNDLE:
                if (!dest.isHumbleBundle()) return false;
                break;
            case ORIGIN:
                if (!dest.isOrigin()) return false;
                break;
        }

        return deal.discountPercent >= dest.getPercentThreshold();
    }

    private void broadcastDeal(Deal deal) throws SQLException {
        for (GameDealDestination dest: dao.queryForAll()) {
            try {
                if (!shouldSend(dest, deal))
                    continue;

                MessageChannel channel;
                if (dest.getGuildId() == 0L) {
                    User user = bot.getJda().getUserById(dest.getChannelId());
                    if (user == null) {
                        dao.delete(dest);
                        continue;
                    }

                    channel = user.openPrivateChannel().complete();
                } else {
                    channel = bot.getJda().getTextChannelById(dest.getChannelId());
                    if (channel == null) {
                        dao.delete(dest);
                        continue;
                    }
                }

                try {
                    channel.sendMessage(deal.rendered).queue();
                } catch (PermissionException ignored) {}
            } catch (Exception e) {
                logger.error("Failed broadcasting deal to destination {}", e, dest.getChannelId());
            }
        }
    }

    private static String filterDescription(String desc) {
        Matcher matcher = IMAGE_PATTERN.matcher(desc);
        desc = matcher.replaceAll("");
        desc = matcher.usePattern(HEADER_PATTERN).reset(desc).replaceAll("**__$2__**");
        desc = matcher.usePattern(MULTI_NEWLINE_PATTERN).reset(desc).replaceAll("\n\n");

        if (desc.length() <= 2048)
            return desc;
        else
            return desc.substring(0, 2045) + "...";
    }

    @Perm.Owner
    @Command(name = "gdtest", desc = "Test GameDeal providers by sending all deals found.", thread = true)
    public void cmdGdTest(Context ctx) {
        ctx.send("Checking...").queue();

        for (Deal deal: checkSteam())
            ctx.send(deal.rendered).queue();
        for (Deal deal: checkHumbleBundle())
            ctx.send(deal.rendered).queue();

        ctx.send("Done.").queue();
    }

    @Command(name = "gamedeal", desc = "Manage your GameDeal settings.", thread = true,
            usage = "[action] {args...}", aliases = {"game_deal", "gd"})
    public void cmdGameDeal(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        if (invoked.equals("subscribe") || invoked.equals("set"))
            cmdCSubSet(ctx);
        else if (invoked.equals("min"))
            cmdMin(ctx);
        else if (invoked.equals("steam"))
            cmdSteam(ctx);
        else if (invoked.equals("humble"))
            cmdHumble(ctx);
        else if (invoked.equals("disable"))
            cmdDisable(ctx);
        else
            ctx.send(NO_COMMAND).queue();
    }

    private GameDealDestination getSettings(Context ctx) throws SQLException, PassException {
        if (ctx.channel instanceof PrivateChannel) {
            GameDealDestination settings = dao.queryForId(ctx.author.getIdLong());

            if (settings == null) {
                ctx.send(Emotes.getFailure() + " You aren't subscribed to GameDeal in this DM!").queue();
                throw new PassException();
            } else {
                return settings;
            }
        } else {
            GameDealDestination settings = dao.queryBuilder()
                    .where()
                    .eq("guildId", ctx.guild.getIdLong())
                    .queryForFirst();

            if (settings == null) {
                ctx.send(Emotes.getFailure() + " This server hasn't subscribed to GameDeal!").queue();
                throw new PassException();
            } else {
                return settings;
            }
        }
    }

    private void cmdCSubSet(Context ctx) throws SQLException {
        GameDealDestination settings;

        if (ctx.channel instanceof PrivateChannel) {
            settings = dao.queryForId(ctx.author.getIdLong());

            if (settings == null) {
                settings = new GameDealDestination(ctx.author.getIdLong(), 0L,
                        true, true, true, (short)50);

                dao.createOrUpdate(settings);
                ctx.send(Emotes.getSuccess() + " You are now subscribed in this DM.").queue();
            } else {
                ctx.send(Emotes.getFailure() + " Already subscribed in this DM!").queue();
            }

            for (Deal deal: lastCheckResult) {
                if (shouldSend(settings, deal))
                    ctx.channel.sendMessage(deal.rendered).queue();
            }
        } else {
            TextChannel channel;
            if (ctx.message.getMentionedChannels().size() > 0) {
                channel = ctx.message.getMentionedChannels().get(0);
            } else {
                ctx.send(Emotes.getFailure() + " You must specify a #channel!").queue();
                return;
            }
            if (!channel.canTalk()) {
                ctx.send(Emotes.getFailure() + " I can't talk in that channel!").queue();
                return;
            }

            settings = dao.queryBuilder()
                    .where()
                    .eq("guildId", ctx.guild.getIdLong())
                    .queryForFirst();

            if (settings == null)
                settings = new GameDealDestination(channel.getIdLong(), ctx.guild.getIdLong(),
                        true, true, true, (short)50);
            else
                settings.setChannelId(channel.getIdLong());

            dao.createOrUpdate(settings);
            ctx.send(Emotes.getSuccess() + " This server is now subscribed to GameDeal in " +
                    channel.getAsMention() + '.').queue();

            for (Deal deal: lastCheckResult) {
                if (shouldSend(settings, deal))
                    channel.sendMessage(deal.rendered).queue();
            }
        }
    }

    private void cmdMin(Context ctx) throws SQLException {
        short percent;
        String arg2;
        if (ctx.args.size() < 2 || (arg2 = ctx.args.get(1)).isEmpty() ||
                !Strings.is4Digits(arg2) || (percent = Short.parseShort(arg2)) > 100 ||
                percent < 1) {
            ctx.send(Emotes.getFailure() + " You must specify a valid percentage!").queue();
            return;
        }

        GameDealDestination settings = getSettings(ctx);
        settings.setPercentThreshold(percent);
        dao.update(settings);

        ctx.send(Emotes.getSuccess() + " Minimum discount is now **" + percent + "%**.").queue();
    }

    private void cmdSteam(Context ctx) throws SQLException {
        GameDealDestination settings = getSettings(ctx);
        settings.setSteam(!settings.isSteam());
        dao.update(settings);

        ctx.send(Emotes.getSuccess() + " Steam deals are now **" + (settings.isSteam() ? "on": "off") +
                "**.").queue();
    }

    private void cmdHumble(Context ctx) throws SQLException {
        GameDealDestination settings = getSettings(ctx);
        settings.setHumbleBundle(!settings.isHumbleBundle());
        dao.update(settings);

        ctx.send(Emotes.getSuccess() + " Humble Bundle deals are now **" + (settings.isSteam() ? "on": "off") +
                "**.").queue();
    }

    private void cmdDisable(Context ctx) throws SQLException {
        GameDealDestination settings = getSettings(ctx);
        dao.delete(settings);

        if (ctx.channel instanceof PrivateChannel)
            ctx.send(Emotes.getSuccess() + " You are no longer subscribed to GameDeal.").queue();
        else
            ctx.send(Emotes.getSuccess() + " This serve is no longer subscribed to GameDeal.").queue();
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
        private MessageEmbed rendered;

        private Deal(DealSource source, boolean free, String title, String description, String link, String imageURL, float price, short discountPercent) {
            this.source = source;
            this.free = free;
            this.title = title;
            this.description = description;
            this.link = link;
            this.imageURL = imageURL;
            this.price = price;
            this.discountPercent = discountPercent;
            this.rendered = renderDeal(this);
        }

        public int hashCode() {
            return (source.name + free + title + link + price + discountPercent)
                    .hashCode();
        }
    }

    @DatabaseTable(tableName = "broadcasted_gamedeals")
    private static class BroadcastedDeal {
        @DatabaseField(id = true, canBeNull = false)
        private int hashCode;

        public BroadcastedDeal() {}

        private BroadcastedDeal(int h) {
            hashCode = h;
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
