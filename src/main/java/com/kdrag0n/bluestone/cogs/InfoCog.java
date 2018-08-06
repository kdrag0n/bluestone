package com.kdrag0n.bluestone.cogs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kdrag0n.bluestone.Emotes;
import com.kdrag0n.bluestone.util.NullValueWrapper;

import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import okhttp3.Request;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.kdrag0n.bluestone.util.Strings.statify;
import static com.kdrag0n.bluestone.util.Strings.str;

public class InfoCog extends com.kdrag0n.bluestone.Cog {
    private static final Logger logger = LogManager.getLogger(InfoCog.class);

    private static final int[] CHAR_NO_PREVIEW = {65279};
    private static final byte[] DIRECTIONALITY_NO_PREVIEW = {Character.DIRECTIONALITY_WHITESPACE, Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE};

    static final String SHRUG = "¯\\_(ツ)_/¯";
    private final LoadingCache<String, EmbedBuilder> ipInfoCache = CacheBuilder.newBuilder()
            .maximumSize(36)
            .expireAfterAccess(6, TimeUnit.HOURS)
            .build(new CacheLoader<String, EmbedBuilder>() {
                @Override
                public EmbedBuilder load(@Nonnull String key) throws IOException {
                    String uri = "https://freegeoip.net/json/" + key;
                    JSONObject data = new JSONObject(com.kdrag0n.bluestone.Bot.http.newCall(new Request.Builder()
                            .get()
                            .url(uri)
                            .build()).execute().body().string());

                    String rdns;
                    try {
                        rdns = InetAddress.getByName(data.getString("ip")).getCanonicalHostName();
                    } catch (UnknownHostException e) {
                        rdns = "Couldn't find host";
                    }

                    return new EmbedBuilder()
                            .setColor(randomColor())
                            .addField("IP", data.getString("ip"), true)
                            .addField("Reverse DNS", rdns, true)
                            .addField("Country", String.format("%s (%s)",
                                    data.getString("country_name"),
                                    data.getString("country_code")), true)
                            .addField("Region", "WIP", true)
                            .addField("City", data.optString("city", SHRUG), true)
                            .addField("ZIP Code", data.optString("zip_code", SHRUG), true)
                            .addField("Timezone", data.optString("time_zone", SHRUG), true)
                            .addField("Longitude", data.optString("longitude", SHRUG), true)
                            .addField("Latitude", data.optString("latitude", SHRUG), true)
                            .addField("Metro Code", data.optInt("metro_code") != 0 ?
                                    data.optString("metro_code") :
                                    SHRUG, true);
                }
            });

    public InfoCog(com.kdrag0n.bluestone.Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Information";
    }

    public String getDescription() {
        return "All sorts of information.";
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "charinfo", desc = "Get the Unicode character info for some text.", usage = "[text]")
    public void cmdCharInfo(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need some text!");
            return;
        }

        com.kdrag0n.bluestone.util.Paginator pager = new com.kdrag0n.bluestone.util.Paginator();
        PrimitiveIterator.OfInt iterator = new com.kdrag0n.bluestone.util.UnisafeString(StringUtils.replace(ctx.rawArgs, "\n", "")).chars();

        while (iterator.hasNext()) {
            int codepoint = iterator.nextInt();
            final String fmt = ArrayUtils.contains(CHAR_NO_PREVIEW, codepoint) ||
                    ArrayUtils.contains(DIRECTIONALITY_NO_PREVIEW, Character.getDirectionality(codepoint)) ?
                    "U+%04X %2$s %1$c" : "U+%04X %2$s %1$c (`%1$c`)";

            pager.addLine(String.format(fmt, codepoint, Character.getName(codepoint)));
        }

        List<String> pages = pager.getPages();
        if (pages.size() > 4) {
            pages = pages.subList(0, 4);
            ctx.send("Result trimmed to 4 pages.").queue();
        }

        for (String page: pages) {
            ctx.send(page).queue();
        }
    }

    private int mCount(GuildImpl g, com.kdrag0n.bluestone.ShardUtil.ObjectFunctionBool<Member> fn) {
        int c = 0;

        for (Member member: g.getMembersMap().valueCollection()) {
            if (fn.apply(member))
                c++;
        }

        return c;
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "xstats", desc = "Get a lot of extended statistics about me.", aliases = {"xstatistics", "xinfo"})
    public void cmdXInfo(com.kdrag0n.bluestone.Context ctx) {
        ctx.channel.sendTyping().queue();
        com.kdrag0n.bluestone.ShardUtil shardUtil = bot.shardUtil;

        Map<String, TIntList> stats = new LinkedHashMap<String, TIntList>() {{
            put("Members per Server", shardUtil.guildNums(g -> g.getMembersMap().size()));
            put("Online Members per Server", shardUtil.guildNums(g ->
                    mCount(g, m -> m.getOnlineStatus() == OnlineStatus.ONLINE)));
            put("Text Channels per Server", shardUtil.guildNums(g -> g.getTextChannelsMap().size()));
            put("Voice Channels per Server", shardUtil.guildNums(g -> g.getVoiceChannelsMap().size()));
            put("Categories per Server", shardUtil.guildNums(g -> g.getCategoriesMap().size()));
            put("Roles per Server", shardUtil.guildNums(g -> g.getRolesMap().size()));
            put("Custom Emotes per Server", shardUtil.guildNums(g -> g.getEmoteMap().size()));
        }};

        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/goldmine")
                .setColor(randomColor())
                .setDescription("¯\\_(ツ)_/¯")
                .setFooter("Also try the info command!", null)
                .setTimestamp(Instant.now());

        for (Map.Entry<String, TIntList> stat: stats.entrySet()) {
            emb.addField(stat.getKey(), com.kdrag0n.bluestone.util.Strings.statify(stat.getValue()), true);
        }

        int exclusive = shardUtil.guildCount(g -> mCount(g, m -> m.getUser().isBot()) < 2);
        String excText = String.format("%d (%.2f%%)", exclusive,
                ((float) exclusive / (float) shardUtil.getGuildCount()) * 100f);

        int big = shardUtil.guildCount(g -> g.getMembersMap().size() >= 250);
        String bigText = String.format("%d (%.2f%%)", big, ((float) big / (float) shardUtil.getGuildCount()) * 100f);

        int partnered = shardUtil.guildCount(g -> g.getSplashUrl() != null || g.getRegion().isVip());
        String partneredText = String.format("%d (%.2f%%)", partnered,
                ((float) partnered / (float) shardUtil.getGuildCount()) * 100f);

        emb.addBlankField(false)
                .addField("Total Queue Size", com.kdrag0n.bluestone.util.Strings.str(shardUtil.getTrackCount()), true)
                .addField("Servers where I'm the only bot", excText, true)
                .addField("Big Servers", bigText, true)
                .addField("Partnered Servers", partneredText, true);

        ctx.send(emb.build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "serverinfo", desc = "Get loads of info about this server.", guildOnly = true, aliases = {"sinfo", "server", "guild", "guildinfo", "ginfo"})
    public void cmdGuildInfo(com.kdrag0n.bluestone.Context ctx) {
        GuildImpl guild = (GuildImpl) ctx.guild;

        TObjectIntMap<com.kdrag0n.bluestone.enums.MemberStatus> statusMap = new TObjectIntHashMap<>();
        for (Member member: ctx.guild.getMembers()) {
            com.kdrag0n.bluestone.enums.MemberStatus status = com.kdrag0n.bluestone.enums.MemberStatus.from(member);
            statusMap.adjustOrPutValue(status, 1, 1);
        }
        StringBuilder membersText = new StringBuilder();
        statusMap.forEachEntry((k, v) -> {
            membersText.append(com.kdrag0n.bluestone.Emotes.getStatusWithText(k))
                    .append(':').append(' ')
                    .append(v)
                    .append('\n');
            return true;
        });
        membersText.append(com.kdrag0n.bluestone.Emotes.getPlus())
                .append(" Total: ")
                .append(ctx.guild.getMembers().size());

        TextChannel memberChan = defaultReadableChannel(ctx.member);

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(NullValueWrapper.val(guild.getSelfMember().getColor()).or(Color.WHITE))
                .setAuthor(guild.getName(), null,
                        NullValueWrapper.val(guild.getIconUrl()).or(ctx.jda.getSelfUser()::getEffectiveAvatarUrl))
                .setFooter("Server created at", guild.getIconUrl())
                .setTimestamp(guild.getCreationTime())
                .addField("ID", guild.getId(), true)
                .addField("Channels", com.kdrag0n.bluestone.util.Strings.str(guild.getTextChannelsMap().size() + guild.getVoiceChannelsMap().size()), true)
                .addField("Roles", com.kdrag0n.bluestone.util.Strings.str(guild.getRolesMap().size()), true)
                .addField("Emotes", com.kdrag0n.bluestone.util.Strings.str(guild.getEmoteMap().size()), true)
                .addField("Region", guild.getRegion().getName(), true)
                .addField("Owner", guild.getOwner().getAsMention(), true)
                .addField("Default Channel (for you)", memberChan == null ? "None" : memberChan.getAsMention(), true)
                .addField("Admins Need 2FA?", guild.getRequiredMFALevel().getKey() == 1 ? "Yes" : "No", true)
                .addField("Content Scan Level", guild.getExplicitContentLevel().getDescription(), true)
                .addField("Verification Level", WordUtils.capitalize(guild.getVerificationLevel().name().toLowerCase()
                        .replace('_', ' ')), true)
                .addField("Members", membersText.toString(), true)
                .setThumbnail(guild.getIconUrl());

        ctx.send(emb.build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "ipinfo", desc = "Get information about an IP or domain.", aliases = {"ip"}, thread = true)
    public void cmdIpInfo(com.kdrag0n.bluestone.Context ctx) throws Throwable {
        if (ctx.args.empty) {
            ctx.fail("I need an IP or domain!");
            return;
        } else if (!com.kdrag0n.bluestone.util.Strings.isIPorDomain(ctx.rawArgs)) {
            ctx.fail("Invalid domain, IPV4, or IPV6 address!");
            return;
        }

        try {
            ctx.send(ipInfoCache.get(ctx.rawArgs)
                    .setTimestamp(Instant.now())
                    .setAuthor("IP Data", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                    .build()).queue();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                logger.error("ipinfo API error", e.getCause());
                ctx.fail("Request failed.");
            } else {
                throw e.getCause();
            }
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "weather", desc = "Get the weather for a place.", usage = "[city]")
    public void cmdWeather(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need a place to get the weather for!");
            return;
        } else if (!bot.getKeys().has("openweathermap")) {
            ctx.fail("My owner hasn't set this feature up!");
            return;
        }

        com.kdrag0n.bluestone.Bot.http.newCall(new Request.Builder()
                .get()
                .url(com.kdrag0n.bluestone.util.Strings.buildQueryUrl("http://api.openweathermap.org/data/2.5/find",
                        "q", ctx.rawArgs,
                        "type", "like",
                        "units", "imperial"))
                .header("X-API-Key", bot.getKeys().getString("openweathermap"))
                .build()).enqueue(com.kdrag0n.bluestone.Bot.callback(response -> {
            if (!response.isSuccessful()) {
                throw new com.kdrag0n.bluestone.errors.PassException();
            }

            JSONObject root = new JSONObject(response.body().string());

            if (root.optInt("count") > 0 && root.getJSONArray("list").length() > 0) {
                JSONObject data = root.getJSONArray("list").getJSONObject(0);
                JSONObject wind = NullValueWrapper.val(data.optJSONObject("wind")).or(com.kdrag0n.bluestone.Bot.EMPTY_JSON_OBJECT);
                JSONObject main = NullValueWrapper.val(data.optJSONObject("main")).or(com.kdrag0n.bluestone.Bot.EMPTY_JSON_OBJECT);
                JSONObject sys = NullValueWrapper.val(data.optJSONObject("sys")).or(com.kdrag0n.bluestone.Bot.EMPTY_JSON_OBJECT);
                JSONObject clouds = NullValueWrapper.val(data.optJSONObject("clouds")).or(com.kdrag0n.bluestone.Bot.EMPTY_JSON_OBJECT);
                JSONObject condition = NullValueWrapper.val(data.optJSONArray("weather")).or(com.kdrag0n.bluestone.Bot.EMPTY_JSON_ARRAY).optJSONObject(0);

                EmbedBuilder emb = new EmbedBuilder()
                        .setAuthor("Weather for " + data.getString("name"), null,
                                ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                        .addField("💨 Wind", wind.optDouble("speed") +
                                " mph (direction: " + wind.optInt("deg") + "°)", true)
                        .addField("💧 Humidity",
                                com.kdrag0n.bluestone.util.Strings.str(main.optDouble("humidity")) + '%', true)
                        .addField("🌅 Sunrise Time", sys.optLong("sunrise") == 0L ? InfoCog.SHRUG :
                                new Date(sys.optLong("sunrise")).toString(), true)
                        .addField("🌇 Sunset Time", sys.optLong("sunset") == 0L ? InfoCog.SHRUG :
                                new Date(sys.optLong("sunset")).toString(), true)
                        .addField("☀ Today's High", com.kdrag0n.bluestone.util.Strings.str(main.optDouble("temp_max")) + "°F", true)
                        .addField("❄ Today's Low", com.kdrag0n.bluestone.util.Strings.str(main.optDouble("temp_min")) + "°F", true)
                        .addField("🌡 Temperature Now", com.kdrag0n.bluestone.util.Strings.str(main.optDouble("temp")) + "°F", true)
                        .addField("☁ Cloudiness", clouds.optInt("all") + "%", true)
                        .addField("⏬ Pressure", main.optInt("pressure") + " hPa", true)
                        .addField("🏙 Condition", "**" + condition.optString("main", InfoCog.SHRUG) +
                                "** - " + condition.optString("description", InfoCog.SHRUG), true)
                        .setColor(com.kdrag0n.bluestone.util.GraphicsUtils.interpolateColors(Color.BLUE, Color.RED,
                                Math.max(Math.min(main.optDouble("temp") / 106, 1.0), 0.0)));

                if (data.getLong("dt") < System.currentTimeMillis() - 157784630000L)
                    emb.setFooter("Data fetched at", null)
                            .setTimestamp(Instant.now());
                else
                    emb.setFooter("Data updated at", null)
                            .setTimestamp(Instant.ofEpochMilli(data.getLong("dt")));

                if (condition.has("icon"))
                    emb.setThumbnail("https://openweathermap.org/img/w/" + condition.getString("icon") + ".png");

                ctx.send(emb.build()).queue();
            }
        }, e -> ctx.send(Emotes.getFailure() + " Failed to get weather for that location!").queue()));
    }
}
