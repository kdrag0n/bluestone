package com.kdrag0n.bluestone.modules;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.types.MemberStatus;
import com.kdrag0n.bluestone.errors.PassException;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.util.GraphicsUtils;
import com.kdrag0n.bluestone.util.Paginator;
import com.kdrag0n.bluestone.util.Strings;
import com.kdrag0n.bluestone.util.UnicodeString;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import okhttp3.Request;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;
import static com.kdrag0n.bluestone.util.Strings.statify;
import static com.kdrag0n.bluestone.util.Strings.str;

public class InfoModule extends Module {
    private static final Logger logger = LoggerFactory.getLogger(InfoModule.class);

    private static final int[] CHAR_NO_PREVIEW = { 65279 };
    private static final byte[] DIRECTIONALITY_NO_PREVIEW = { Character.DIRECTIONALITY_WHITESPACE,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE, Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE };

    private final LoadingCache<Pair<String, String>, EmbedBuilder> ipInfoCache = CacheBuilder.newBuilder().maximumSize(36)
            .expireAfterAccess(6, TimeUnit.HOURS).build(new CacheLoader<Pair<String, String>, EmbedBuilder>() {
                @Override
                public EmbedBuilder load(@Nonnull Pair<String, String> args) throws IOException {
                    String ip = args.getLeft();
                    String apiKey = args.getRight();

                    String uri = "http://api.ipstack.com/" + ip + "?access_key=" + apiKey + "&hostname=1";
                    JSONObject data = new JSONObject(
                            bot.http.newCall(new Request.Builder().get().url(uri).build()).execute().body().string());

                    return new EmbedBuilder().setColor(randomColor()).addField("IP", ip, true)
                            .addField("Reverse DNS", data.optString("hostname", "Error resolving host"), true)
                            .addField("Continent",
                                    String.format("%s (%s)", data.optString("continent_name", "?"),
                                            data.optString("continent_code", "?")), true)
                            .addField("Country",
                                    String.format("%s %s (%s)",
                                            val(data.optJSONObject("location")).or(new JSONObject())
                                                    .optString("country_flag_emoji", ""),
                                            data.optString("country_name", "?"),
                                            data.optString("country_code", "?")), true)
                            .addField("Region",
                                    String.format("%s (%s)", data.optString("region_name", "?"),
                                            data.optString("region_code", "?")), true)
                            .addField("City", data.optString("city", "?"), true)
                            .addField("ZIP Code", data.optString("zip", "?"), true)
                            .addField("Timezone", val(data.optJSONObject("time_zone")).or(new JSONObject())
                                    .optString("code", "?"), true)
                            .addField("Longitude", data.optString("longitude", "?"), true)
                            .addField("Latitude", data.optString("latitude", "?"), true)
                            .addField("ISP", String.format("%s (AS%s)",
                                            val(data.optJSONObject("connection")).or(new JSONObject())
                                                    .optString("isp", "?"),
                                            val(data.optJSONObject("connection")).or(new JSONObject())
                                                    .optInt("asn", 0)), true);
                }
            });

    public InfoModule(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Information";
    }

    @Command(name = "charinfo", desc = "Get the Unicode character info for some text.", usage = "[text]")
    public void cmdCharInfo(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need some text!");
            return;
        }

        Paginator pager = new Paginator();
        PrimitiveIterator.OfInt iterator = new UnicodeString(StringUtils.replace(ctx.rawArgs, "\n", "")).chars();

        while (iterator.hasNext()) {
            int codepoint = iterator.nextInt();
            final String fmt = ArrayUtils.contains(CHAR_NO_PREVIEW, codepoint)
                    || ArrayUtils.contains(DIRECTIONALITY_NO_PREVIEW, Character.getDirectionality(codepoint))
                            ? "U+%04X %2$s %1$c"
                            : "U+%04X %2$s %1$c (`%1$c`)";

            pager.addLine(String.format(fmt, codepoint, Character.getName(codepoint)));
        }

        List<String> pages = pager.getPages();
        if (pages.size() > 4) {
            pages = pages.subList(0, 4);
            ctx.send("Result trimmed to 4 pages.").queue();
        }

        for (String page : pages) {
            ctx.send(page).queue();
        }
    }

    private int mCount(Guild g, ShardedBot.ObjectFunctionBool<Member> fn) {
        int c = 0;

        for (Member member : g.getMemberCache()) {
            if (fn.apply(member))
                c++;
        }

        return c;
    }

    @Command(name = "xstats", desc = "Get a lot of extended statistics about me.", aliases = { "xstatistics", "xinfo" })
    public void cmdXInfo(Context ctx) {
        ctx.channel.sendTyping().queue();

        Map<String, TLongList> stats = new LinkedHashMap<String, TLongList>() {
            {
                put("Members per Server", bot.guildNums(g -> g.getMemberCache().size()));
                put("Online Members per Server",
                        bot.guildNums(g -> mCount(g, m -> m.getOnlineStatus() == OnlineStatus.ONLINE)));
                put("Text Channels per Server", bot.guildNums(g -> g.getTextChannelCache().size()));
                put("Voice Channels per Server", bot.guildNums(g -> g.getVoiceChannelCache().size()));
                put("Categories per Server", bot.guildNums(g -> g.getCategoryCache().size()));
                put("Roles per Server", bot.guildNums(g -> g.getRoleCache().size()));
                put("Custom Emotes per Server", bot.guildNums(g -> g.getEmoteCache().size()));
            }
        };

        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/goldmine").setColor(randomColor())
                .setDescription("¯\\_(ツ)_/¯").setFooter("Also try the info command!", null).setTimestamp(Instant.now());

        for (Map.Entry<String, TLongList> stat : stats.entrySet()) {
            emb.addField(stat.getKey(), statify(stat.getValue()), true);
        }

        int exclusive = bot.guildCount(g -> mCount(g, m -> m.getUser().isBot()) < 2);
        String excText = String.format("%d (%.2f%%)", exclusive,
                ((float) exclusive / (float) bot.getGuildCount()) * 100f);

        int big = bot.guildCount(g -> g.getMemberCache().size() >= 250);
        String bigText = String.format("%d (%.2f%%)", big, ((float) big / (float) bot.getGuildCount()) * 100f);

        int partnered = bot.guildCount(g -> g.getSplashUrl() != null || g.getRegion().isVip());
        String partneredText = String.format("%d (%.2f%%)", partnered,
                ((float) partnered / (float) bot.getGuildCount()) * 100f);

        emb.addBlankField(false).addField("Total Queue Size", str(bot.getTrackCount()), true)
                .addField("Servers where I'm the only bot", excText, true).addField("Big Servers", bigText, true)
                .addField("Partnered Servers", partneredText, true);

        ctx.send(emb.build()).queue();
    }

    @Command(name = "serverinfo", desc = "Get loads of info about this server.", guildOnly = true, aliases = { "sinfo",
            "server", "guild", "guildinfo", "ginfo" })
    public void cmdGuildInfo(Context ctx) {
        Guild guild = ctx.guild;

        TObjectIntMap<MemberStatus> statusMap = new TObjectIntHashMap<>();
        for (Member member : ctx.guild.getMembers()) {
            MemberStatus status = MemberStatus.from(member);
            statusMap.adjustOrPutValue(status, 1, 1);
        }
        StringBuilder membersText = new StringBuilder();
        statusMap.forEachEntry((k, v) -> {
            membersText.append(Emotes.getStatusWithText(k))
                    .append(": ")
                    .append(v)
                    .append('\n');
            return true;
        });
        membersText.append(Emotes.getPlus())
                .append(" Total: ")
                .append(ctx.guild.getMembers().size());

        TextChannel memberChan = defaultReadableChannel(ctx.member);

        EmbedBuilder emb = new EmbedBuilder().setColor(val(guild.getSelfMember().getColor()).or(Color.WHITE))
                .setAuthor(guild.getName(), null,
                        val(guild.getIconUrl()).or(ctx.jda.getSelfUser()::getEffectiveAvatarUrl))
                .setFooter("Server created at", guild.getIconUrl()).setTimestamp(guild.getTimeCreated())
                .addField("ID", guild.getId(), true)
                .addField("Channels", str(guild.getTextChannelCache().size() + guild.getVoiceChannelCache().size()), true)
                .addField("Roles", str(guild.getRoleCache().size()), true)
                .addField("Emotes", str(guild.getEmoteCache().size()), true)
                .addField("Region", guild.getRegion().getName(), true)
                .addField("Owner", guild.getOwner().getAsMention(), true)
                .addField("Default GuildChannel (for you)", memberChan == null ? "None" : memberChan.getAsMention(), true)
                .addField("Admins Need 2FA?", guild.getRequiredMFALevel().getKey() == 1 ? "Yes" : "No", true)
                .addField("Content Scan Level", guild.getExplicitContentLevel().getDescription(), true)
                .addField("Verification Level",
                        WordUtils.capitalize(guild.getVerificationLevel().name().toLowerCase().replace('_', ' ')), true)
                .addField("Members", membersText.toString(), true).setThumbnail(guild.getIconUrl());

        ctx.send(emb.build()).queue();
    }

    @Command(name = "ipinfo", desc = "Get information about an IP or domain.", aliases = { "ip" })
    public void cmdIpInfo(Context ctx) throws Throwable {
        if (ctx.args.empty) {
            ctx.fail("I need an IP or domain!");
            return;
        } else if (!Strings.isIPorDomain(ctx.rawArgs)) {
            ctx.fail("Invalid domain, IPV4, or IPV6 address!");
            return;
        }

        String key = bot.getKeys().optString("ipstack");
        if (key == null) {
            ctx.fail("This bot doesn't have an IPStack API key set up!");
            return;
        }

        try {
            ctx.send(ipInfoCache.get(Pair.of(ctx.rawArgs, key)).setTimestamp(Instant.now())
                    .setAuthor("IP Data", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl()).build()).queue();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                logger.error("ipinfo API error", e.getCause());
                ctx.fail("Request failed.");
            } else {
                throw e.getCause();
            }
        }
    }

    @Command(name = "weather", desc = "Get the weather for a place.", usage = "[city]")
    public void cmdWeather(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need a place to get the weather for!");
            return;
        } else if (!bot.getKeys().has("openweathermap")) {
            ctx.fail("My owner hasn't set this feature up!");
            return;
        }

        bot.http.newCall(new Request.Builder().get()
                .url(Strings.buildQueryUrl("http://api.openweathermap.org/data/2.5/find", "q", ctx.rawArgs, "type",
                        "like", "units", "imperial"))
                .header("X-API-Key", bot.getKeys().getString("openweathermap")).build())
                .enqueue(Bot.callback(response -> {
                    if (!response.isSuccessful()) {
                        throw new PassException();
                    }

                    JSONObject root = new JSONObject(response.body().string());

                    if (root.optInt("count") > 0 && root.getJSONArray("list").length() > 0) {
                        JSONObject data = root.getJSONArray("list").getJSONObject(0);
                        JSONObject wind = val(data.optJSONObject("wind")).or(Bot.EMPTY_JSON_OBJECT);
                        JSONObject main = val(data.optJSONObject("main")).or(Bot.EMPTY_JSON_OBJECT);
                        JSONObject sys = val(data.optJSONObject("sys")).or(Bot.EMPTY_JSON_OBJECT);
                        JSONObject clouds = val(data.optJSONObject("clouds")).or(Bot.EMPTY_JSON_OBJECT);
                        JSONObject condition = val(data.optJSONArray("weather")).or(Bot.EMPTY_JSON_ARRAY)
                                .optJSONObject(0);

                        EmbedBuilder emb = new EmbedBuilder()
                                .setAuthor("Weather for " + data.getString("name"), null,
                                        ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                                .addField("💨 Wind",
                                        wind.optDouble("speed") + " mph (direction: " + wind.optInt("deg") + "°)", true)
                                .addField("💧 Humidity", str(main.optDouble("humidity")) + '%', true)
                                .addField("🌅 Sunrise Time",
                                        sys.optLong("sunrise") == 0L ? "?"
                                                : new Date(sys.optLong("sunrise")).toString(),
                                        true)
                                .addField("🌇 Sunset Time",
                                        sys.optLong("sunset") == 0L ? "?"
                                                : new Date(sys.optLong("sunset")).toString(),
                                        true)
                                .addField("☀ Today's High", str(main.optDouble("temp_max")) + "°F", true)
                                .addField("❄ Today's Low", str(main.optDouble("temp_min")) + "°F", true)
                                .addField("🌡 Temperature Now", str(main.optDouble("temp")) + "°F", true)
                                .addField("☁ Cloudiness", clouds.optInt("all") + "%", true)
                                .addField("⏬ Pressure", main.optInt("pressure") + " hPa", true)
                                .addField("🏙 Condition",
                                        "**" + condition.optString("main", "?") + "** - "
                                                + condition.optString("description", "?"),
                                        true)
                                .setColor(GraphicsUtils.interpolateColors(Color.BLUE, Color.RED,
                                        Math.max(Math.min(main.optDouble("temp") / 106, 1.0), 0.0)));

                        if (data.getLong("dt") < System.currentTimeMillis() - 157784630000L)
                            emb.setFooter("Data fetched at", null).setTimestamp(Instant.now());
                        else
                            emb.setFooter("Data updated at", null)
                                    .setTimestamp(Instant.ofEpochMilli(data.getLong("dt")));

                        if (condition.has("icon"))
                            emb.setThumbnail(
                                    "https://openweathermap.org/img/w/" + condition.getString("icon") + ".png");

                        ctx.send(emb.build()).queue();
                    }
                }, e -> ctx.send(Emotes.getFailure() + " Failed to get weather for that location!").queue()));
    }
}
