package com.khronodragon.bluestone.cogs;

import ch.jamiete.mcping.MinecraftPing;
import ch.jamiete.mcping.MinecraftPingOptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.Cooldown;
import com.khronodragon.bluestone.emotes.DiscordEmoteProvider;
import com.khronodragon.bluestone.enums.BucketType;
import com.khronodragon.bluestone.enums.MemberStatus;
import com.khronodragon.bluestone.sql.ActivePoll;
import com.khronodragon.bluestone.util.*;
import com.sun.management.OperatingSystemMXBean;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.entities.impl.UserImpl;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.utils.MiscUtil;
import okhttp3.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.smartJoin;
import static com.khronodragon.bluestone.util.Strings.statify;
import static com.khronodragon.bluestone.util.Strings.str;
import static com.khronodragon.bluestone.util.Strings.format;

import java.awt.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UtilityCog extends Cog {
    private static final Logger logger = LogManager.getLogger(UtilityCog.class);

    private static final Collection<Permission> PERMS_NEEDED = Permission.getPermissions(473295957L);
    private static final Pattern UNICODE_EMOTE_PATTERN = Pattern.compile("([\\u20a0-\\u32ff\\x{1f000}-\\x{1ffff}\\x{fe4e5}-\\x{fe4ee}])");
    private static final Pattern CUSTOM_EMOTE_PATTERN = Pattern.compile("<:[a-z_]+:([0-9]{17,19})>", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVITE_PATTERN = Pattern
            .compile("^(?:https?://discord(?:app\\.com/invite|\\.gg)/([a-zA-Z0-9]{7}|[a-zA-Z0-9]{16})|([a-zA-Z0-9]{7}|[a-zA-Z0-9]{16}))$");
    private static final Pattern END_RMENTION_PATTERN = Pattern.compile(", [<@&0-9>]*$");
    private static final Pattern CONTIGUOUS_SPACE_PATTERN = Pattern.compile("\\s+");

    private static final Color MC_GREEN = new Color(89, 129, 53);
    private static final Color MC_BROWN = new Color(133, 105, 77);

    private static final int[] CHAR_NO_PREVIEW = {65279};
    private static final byte[] DIRECTIONALITY_NO_PREVIEW = {Character.DIRECTIONALITY_WHITESPACE, Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE};
    private static final Pattern MC_COLOR_PATTERN = Pattern.compile("\\u00a7[4c6e2ab319d5f78lnokmr]");
    private static final JSONArray EMPTY_JSON_ARRAY = new JSONArray();
    static final OperatingSystemMXBean systemBean = (OperatingSystemMXBean)
            ManagementFactory.getOperatingSystemMXBean();

    private static final Fairy fairy = Fairy.create();
    private static final QRCodeWriter qrWriter = new QRCodeWriter();
    private static final Map<EncodeHintType, Object> qrHintMap = new HashMap<EncodeHintType, Object>() {{
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        put(EncodeHintType.CHARACTER_SET, "UTF-8");
    }};
    private final PrettyTime prettyTime = new PrettyTime();
    private final Parser timeParser = new Parser();

    private static final String NO_USER = Emotes.getFailure() + " I need a valid @mention, user ID, or user#discriminator!";
    private static final String INFO_LINKS = "\u200b    \u2022 Use my [invite link]([invite]) to take me to another server!\n" +
            "    \u2022 [Donate](https://patreon.com/kdragon) to help keep me alive!\n" +
            "    \u2022 Go to [my website](https://khronodragon.com/goldmine/) for help!\n" +
            "    \u2022 Join my [support server](https://discord.gg/sYkwfxA) for even more help.";
    private static final String SHRUG = "¯\\_(ツ)_/¯";
    private final LoadingCache<String, EmbedBuilder> ipInfoCache = CacheBuilder.newBuilder()
            .maximumSize(36)
            .expireAfterAccess(6, TimeUnit.HOURS)
            .build(new CacheLoader<String, EmbedBuilder>() {
                @Override
                public EmbedBuilder load(String key) throws IOException {
                    String uri = "https://freegeoip.net/json/" + key;
                    JSONObject data = new JSONObject(Bot.http.newCall(new Request.Builder()
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
                            .addField("Country", format("{0} ({1})",
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
    private Dao<ActivePoll, Long> pollDao;

    private static final List<ImmutablePair<Pattern, Integer>> MEME_PATTERNS = ImmutableList.of(
            pair("(one does not simply) (.*)", 61579),
            pair("(i don'?t always .*) (but when i do,? .*)", 61532),
            pair("aliens ()(.*)", 101470),
            pair("grumpy cat ()(.*)", 405658),
            pair("(.*),? (\\1 everywhere)", 347390),
            pair("(not sure if .*) (or .*)", 61520),
            pair("(y u no) (.+)", 61527),
            pair("(brace yoursel[^\\s]+) (.*)", 61546),
            pair("(.*) (all the .*)", 61533),
            pair("(.*) (that would be great|that'?d be great)", 563423),
            pair("(.*) (\\w+\\stoo damn .*)", 61580),
            pair("(yo dawg .*) (so .*)", 101716),
            pair("(.*) (.* gonna have a bad time)", 100951),
            pair("(am i the only one around here) (.*)", 259680),
            pair("(what if i told you) (.*)", 100947),
            pair("(.*) (ain'?t nobody got time for? that)", 442575),
            pair("(.*) (i guarantee it)", 10672255),
            pair("(.*) (a+n+d+ it'?s gone)", 766986),
            pair("(.* bats an eye) (.* loses their minds?)", 1790995),
            pair("(back in my day) (.*)", 718432)
    )
            .stream()
            .map(p -> ImmutablePair.of(Pattern.compile(p.getLeft(), Pattern.CASE_INSENSITIVE), p.getRight()))
            .collect(Collectors.toList());

    private final ScriptEngine calcEngine = new ScriptEngineManager().getEngineByName("lua");

    private static<L, R> ImmutablePair<L, R> pair(L l, R r) {
        return ImmutablePair.of(l, r);
    }

    public UtilityCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), ActivePoll.class);
        } catch (SQLException e) {
            logger.warn("Failed to create poll table!", e);
        }

        try {
            pollDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), ActivePoll.class);
        } catch (SQLException e) {
            logger.warn("Failed to create poll DAO!", e);
        }

        try {
            scheduleAllPolls();
        } catch (SQLException e) {
            logger.warn("Error rescheduling previous polls", e);
        }

        InputStream st = Bot.class.getClassLoader().getResourceAsStream("assets/calc.lua");
        try {
            calcEngine.eval(IOUtils.toString(st, StandardCharsets.UTF_8));
        } catch (IOException|ScriptException e) {
            logger.error("Error evaluating calc.lua for calc command", e);
        } finally {
            IOUtils.closeQuietly(st);
        }
    }

    public String getName() {
        return "Utility";
    }

    public String getDescription() {
        return "Essential utility commands, as well as playful ones.";
    }

    private void scheduleAllPolls() throws SQLException {
        for (ActivePoll poll: pollDao.queryForAll())
            schedulePoll(poll);
    }

    @Command(name = "icon", desc = "Get the current server's icon.", guildOnly = true)
    public void cmdIcon(Context ctx) {
        ctx.send(val(ctx.guild.getIconUrl()).or("There's no icon here!")).queue();
    }

    @Command(name = "user", desc = "Get some info about a user.",
            usage = "{user}", aliases = {"userinfo", "whois"}, thread = true)
    public void cmdUser(Context ctx) throws UnsupportedEncodingException {
        User user;
        if (Strings.isMention(ctx.rawArgs) && ctx.message.getMentionedUsers().size() > 0)
            user = ctx.message.getMentionedUsers().get(0);
        else if (Strings.isID(ctx.rawArgs)) {
            try {
                user = ctx.jda.retrieveUserById(Long.parseUnsignedLong(ctx.rawArgs)).complete();
            } catch (ErrorResponseException ignored) {
                user = null;
            }
        } else if (Strings.isTag(ctx.rawArgs)) {
            Collection<User> users;
            switch (ctx.channel.getType()) {
                case TEXT:
                    users = ctx.guild.getMembers().stream().map(Member::getUser).collect(Collectors.toList());
                    break;
                case PRIVATE:
                    users = Arrays.asList(ctx.author, ctx.jda.getSelfUser());
                    break;
                case GROUP:
                    users = ((Group) ctx.channel).getUsers();
                    break;
                default:
                    users = Collections.singletonList(ctx.jda.getSelfUser());
                    break;
            }

            user = users.stream()
                    .filter(u -> getTag(u).contentEquals(ctx.rawArgs))
                    .findFirst()
                    .orElse(null);
        } else if (ctx.rawArgs.length() < 1)
            user = ctx.author;
        else
            user = null;

        if (user == null) {
            ctx.send(NO_USER).queue();
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(getTag(user), user.getEffectiveAvatarUrl(), user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setTimestamp(user.getCreationTime())
                .addField("ID", user.getId(), true)
                .addField("Creation Time", Date.from(user.getCreationTime().toInstant()).toString(), true)
                .setFooter("User created at", null);

        if (user.isBot())
            emb.setDescription("User is " + Emotes.getBotTag());

        if (ctx.guild != null) {
            Member member = ctx.guild.getMember(user);

            if (member != null) {
                if (member.getNickname() != null)
                    emb.addField("Nickname", member.getNickname(), true);

                String status;
                if (member.getGame() == null)
                    status = Emotes.getFullMemberStatus(member);
                else {
                    Game game = member.getGame();

                    if (game.getType() == Game.GameType.STREAMING) {
                        status = Emotes.getMemberStatus(member) + "Streaming [**" + game.getName() +
                                "**](" + game.getUrl() + ")";
                    } else {
                        status = Emotes.getMemberStatus(member) + " Playing [**" + game.getName() +
                                "**](https://google.com/search?q=" + URLEncoder.encode(game.getName(), "UTF-8") +
                                ')';
                    }
                }

                String roleText = member.getRoles().stream()
                        .map(Role::getAsMention)
                        .collect(Collectors.joining(", "));
                if (roleText.length() > 1024) {
                    roleText = END_RMENTION_PATTERN.matcher(roleText.substring(0, 1024))
                            .replaceFirst(", **...too many**");
                } else if (roleText.length() < 1) {
                    roleText = "None";
                }

                emb.setColor(val(member.getColor()).or(Color.WHITE))
                        .addField("Server Join Time",
                                Date.from(member.getJoinDate().toInstant()).toString(), true)
                        .addField("Status", status, true)
                        .addField("Roles", roleText, true);
            }
        }

        ctx.send(emb.build()).queue();
    }

    @Command(name = "serverinfo", desc = "Get loads of info about this server.", guildOnly = true, aliases = {"sinfo", "server", "guild", "guildinfo", "ginfo"})
    public void cmdGuildInfo(Context ctx) {
        GuildImpl guild = (GuildImpl) ctx.guild;

        String roleText = guild.getRoles().stream()
                .filter(r -> !r.isPublicRole())
                .map(Role::getAsMention)
                .collect(Collectors.joining(", "));
        if (roleText.length() > 1024) {
            roleText = END_RMENTION_PATTERN.matcher(roleText.substring(0, 1024))
                    .replaceFirst(", **...too many**");
        } else if (roleText.length() < 1) {
            roleText = "None";
        }

        TObjectIntMap<MemberStatus> statusMap = new TObjectIntHashMap<>();
        for (Member member: ctx.guild.getMembers()) {
            MemberStatus status = MemberStatus.from(member);
            statusMap.adjustOrPutValue(status, 1, 1);
        }
        StringBuilder membersText = new StringBuilder();
        statusMap.forEachEntry((k, v) -> {
            membersText.append(Emotes.getStatusWithText(k))
                    .append(':').append(' ')
                    .append(v)
                    .append('\n');
            return true;
        });
        membersText.append(Emotes.getPlus())
                .append(" Total: ")
                .append(ctx.guild.getMembers().size() - 1);

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(val(guild.getSelfMember().getColor()).or(Color.WHITE))
                .setAuthor(guild.getName(), null,
                        val(guild.getIconUrl()).or(ctx.jda.getSelfUser().getEffectiveAvatarUrl()))
                .setFooter("Server created at", guild.getIconUrl())
                .setTimestamp(guild.getCreationTime())
                .addField("ID", guild.getId(), true)
                .addField("Channels", str(guild.getTextChannelsMap().size() + guild.getVoiceChannelsMap().size()), true)
                .addField("Roles (" + guild.getRolesMap().size() + ')', roleText, true)
                .addField("Emotes", str(guild.getEmoteMap().size()), true)
                .addField("Region", guild.getRegion().getName(), true)
                .addField("Owner", guild.getOwner().getAsMention(), true)
                .addField("Default Channel (for you)", defaultReadableChannel(ctx.member).getAsMention(), true)
                .addField("Admins Need 2FA?", guild.getRequiredMFALevel().getKey() == 1 ? "Yes" : "No", true)
                .addField("Content Scan Level", guild.getExplicitContentLevel().getDescription(), true)
                .addField("Verification Level", WordUtils.capitalize(guild.getVerificationLevel().name().toLowerCase()
                        .replace('_', ' ')), true)
                .addField("Members", membersText.toString(), true)
                .setThumbnail(guild.getIconUrl());

        ctx.send(emb.build()).queue();
    }

    @Command(name = "info", desc = "Get some info about me.", aliases = {"about", "stats", "statistics", "status"})
    public void cmdInfo(Context ctx) {
        ShardUtil shardUtil = bot.getShardUtil();
        double load = systemBean.getSystemLoadAverage();
        String loadAvg;
        if (load == -1.0d)
            loadAvg = "¯\\_(ツ)_/¯";
        else
            loadAvg = str(load);

        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/goldmine")
                .setColor(randomColor())
                .setDescription(Emotes.getCredits() +
                        "\nFor more *statistical* information, use the `xstats` command.")
                .addField("Servers", str(shardUtil.getGuildCount()), true)
                .addField("Uptime", bot.formatUptime(), true)
                .addField("Requests", str(shardUtil.getRequestCount()), true)
                .addField("Threads", str(Thread.activeCount()), true)
                .addField("Memory Used", Bot.formatMemory(), true)
                .addField("CPU Usage", format("{0}% - system {1}%",
                        (int) Math.ceil(systemBean.getProcessCpuLoad() * 100),
                        (int) Math.ceil(systemBean.getSystemCpuLoad() * 100)), true)
                .addField("Load Average", loadAvg, true)
                .addField("Users", str(shardUtil.getUserCount()), true)
                .addField("Channels", str(shardUtil.getChannelCount()), true)
                .addField("Commands", str(new HashSet<>(bot.commands.values()).size()), true)
                .addField("Music Tracks Loaded", str(shardUtil.getTrackCount()), true)
                .addField("Playing Music in", shardUtil.getStreamCount() + " channels", true)
                .addField("Links", StringUtils.replace(INFO_LINKS, "[invite]", ctx.jda.asBot().getInviteUrl(PERMS_NEEDED)), false)
                .setFooter("Serving you from shard " + bot.getShardNum(), null)
                .setTimestamp(Instant.now());

        ctx.send(emb.build()).queue();
    }

    @Command(name = "xstats", desc = "Get a lot of extended statistics about me.", aliases = {"xstatistics", "xinfo"})
    public void cmdXInfo(Context ctx) {
        ctx.channel.sendTyping().queue();
        ShardUtil shardUtil = bot.getShardUtil();

        Map<String, IntStream> stats = new LinkedHashMap<String, IntStream>() {{
            put("Members per Server", shardUtil.getGuildStream().mapToInt(g -> ((GuildImpl) g).getMembersMap().size()));
            put("Online Members per Server", shardUtil.getGuildStream()
                    .mapToInt(g -> (int) ((GuildImpl) g).getMembersMap().valueCollection()
                            .stream()
                            .filter(member -> member.getOnlineStatus() == OnlineStatus.ONLINE)
                            .count()));
            put("Text Channels per Server", shardUtil.getGuildStream().mapToInt(g -> ((GuildImpl) g).getTextChannelsMap().size()));
            put("Voice Channels per Server", shardUtil.getGuildStream().mapToInt(g -> ((GuildImpl) g).getVoiceChannelsMap().size()));
            put("Categories per Server", shardUtil.getGuildStream().mapToInt(g -> ((GuildImpl) g).getCategoriesMap().size()));
            put("Roles per Server", shardUtil.getGuildStream().mapToInt(g -> ((GuildImpl) g).getRolesMap().size()));
            put("Custom Emotes per Server", shardUtil.getGuildStream().mapToInt(g -> ((GuildImpl) g).getEmoteMap().size()));
        }};

        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/goldmine")
                .setColor(randomColor())
                .setDescription("¯\\_(ツ)_/¯")
                .setFooter("Also try the info command!", null)
                .setTimestamp(Instant.now());

        for (Map.Entry<String, IntStream> stat: stats.entrySet()) {
            emb.addField(stat.getKey(), statify(stat.getValue()), true);
        }

        int exclusive = (int) shardUtil.getGuildStream().filter(g -> ((GuildImpl) g).getMembersMap().valueCollection()
                .stream()
                .filter(m -> m.getUser().isBot())
                .count() < 2).count();
        String excText = String.format("%d (%.2f%%)", exclusive, ((float) exclusive / (float) shardUtil.getGuildCount()) * 100f);

        int big = (int) shardUtil.getGuildStream()
                .filter(g -> ((GuildImpl) g).getMembersMap().size() >= 250)
                .count();
        String bigText = String.format("%d (%.2f%%)", big, ((float) big / (float) shardUtil.getGuildCount()) * 100f);

        int partnered = (int) shardUtil.getGuildStream()
                .filter(g -> g.getSplashUrl() != null || g.getRegion().isVip())
                .count();
        String partneredText = String.format("%d (%.2f%%)", partnered, ((float) partnered / (float) shardUtil.getGuildCount()) * 100f);

        emb.addBlankField(false)
                .addField("Total Queue Size", str(shardUtil.getTrackCount()), true)
                .addField("Servers where I'm the only bot", excText, true)
                .addField("Big Servers", bigText, true)
                .addField("Partnered Servers", partneredText, true);

        ctx.send(emb.build()).queue();
    }

    @Command(name = "invite", desc = "Generate an invite link for myself or another bot.", aliases = {"addbot", "join"})
    public void cmdInvite(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send('<' + ctx.jda.asBot().getInviteUrl(PERMS_NEEDED) + '>').queue();
        } else {
            if (!Strings.isID(ctx.rawArgs)) {
                ctx.send(Emotes.getFailure() + " Invalid ID!").queue();
                return;
            } else if (ctx.rawArgs.equals(ctx.jda.getSelfUser().getId())) {
                ctx.send('<' + ctx.jda.asBot().getInviteUrl(PERMS_NEEDED) + '>').queue();
            }

            ctx.send(format("<https://discordapp.com/api/oauth2/authorize?client_id={0}&scope=bot&permissions=3072>",
                    ctx.rawArgs)).queue();
        }
    }

    @Command(name = "home", desc = "Get my \"contact\" info.", aliases = {"website", "web", "support"})
    public void cmdHome(Context ctx) {
        ctx.send("**Website**: <https://khronodragon.com/goldmine>\n" +
                "**Forums**: <https://forums.khronodragon.com>\n" +
                "**Support Server**: <https://discord.gg/sYkwfxA>\n" +
                "**Patreon**: <https://patreon.com/kdragon>").queue();
    }

    @Command(name = "poll", desc = "Start a poll, with reactions.", usage = "[emotes] [question] [time]", guildOnly = true)
    public void cmdPoll(Context ctx) {
        if (ctx.args.size() < 1) {
            ctx.send(Emotes.getFailure() + " Missing question, emotes, and time (like `5 minutes`)!").queue();
            return;
        }

        StringBuilder qBuilder = new StringBuilder(ctx.rawArgs);
        List<DateGroup> groups = timeParser.parse(ctx.rawArgs);
        Collections.reverse(groups);

        Date date = null;
        for (DateGroup group: groups) {
            if (!group.getDates().isEmpty()) {
                date = group.getDates().get(0);
                int pos = qBuilder.lastIndexOf(group.getText());
                qBuilder.replace(pos, pos + group.getText().length(), "");
                break;
            }
        }

        if (date == null || date.getTime() < System.currentTimeMillis()) {
            ctx.send(Emotes.getFailure() +
                    " Invalid time! I take formats like `1 week`, `5 minutes`, or `2 years`.").queue();
            return;
        }

        final Date finalDate = date;
        String preQuestion = qBuilder.toString().trim();

        Set<String> unicodeEmotes = RegexUtils.matchStream(UNICODE_EMOTE_PATTERN, preQuestion)
                .map(MatchResult::group).collect(Collectors.toSet());
        Set<Emote> customEmotes = RegexUtils.matchStream(CUSTOM_EMOTE_PATTERN, preQuestion)
                .map(m -> ctx.jda.getEmoteById(m.group(1)))
                .collect(Collectors.toSet());

        if (customEmotes.contains(null)) {
            customEmotes.remove(null);
        } else if (unicodeEmotes.size() + customEmotes.size() < 2) {
            ctx.send(Emotes.getFailure() + " You need at least 2 emotes to start a poll!").queue();
            return;
        }

        final Matcher _m = UNICODE_EMOTE_PATTERN.matcher(preQuestion);
        preQuestion = _m.replaceAll("");
        preQuestion = _m.usePattern(DiscordEmoteProvider.CUSTOM_EMOTE_PATTERN).reset(preQuestion).replaceAll("");
        preQuestion = _m.usePattern(CONTIGUOUS_SPACE_PATTERN).reset(preQuestion).replaceAll(" ");
        final String question = preQuestion.trim();

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(ctx.member.getEffectiveName() + " is polling...",
                        null, ctx.author.getEffectiveAvatarUrl())
                .setColor(ctx.member.getColor())
                .setDescription(question)
                .appendDescription("\n\n")
                .appendDescription("**⌛ Reactions are being added...**");

        ctx.send(embed.build()).queue(msg -> {
            for (String emote: unicodeEmotes) {
                msg.addReaction(emote).queue();
            }
            for (Emote emote: customEmotes) {
                msg.addReaction(emote).queue();
            }

            ActivePoll poll = new ActivePoll(msg.getIdLong(), msg.getChannel().getIdLong(), finalDate);
            bot.threadExecutor.execute(() -> {
                try {
                    pollDao.create(poll);
                } catch (SQLException e) {
                    logger.error("Error persisting poll", e);
                }
            });

            embed.setDescription(question)
                    .appendDescription("\n\n")
                    .appendDescription("**✅ Go ahead and vote!**");

            schedulePoll(poll);

            bot.getScheduledExecutor().schedule(() ->
                            msg.editMessage(embed.build()).queue(),
                    (unicodeEmotes.size() + customEmotes.size()) * (int) (ctx.jda.getPing() * 1.92),
                    TimeUnit.MILLISECONDS);
        });
    }

    private void schedulePoll(final ActivePoll poll) {
        long calculatedTime = poll.getEndTime().getTime() - System.currentTimeMillis();

        if (bot.getJda().getTextChannelById(poll.getChannelId()) == null)
            return;

        bot.getScheduledExecutor().schedule(() -> {
            TextChannel channel = bot.getJda().getTextChannelById(poll.getChannelId());

            try {
                if (channel == null)
                    return;

                Message message;
                try {
                    message = channel.getMessageById(poll.getMessageId()).complete();
                } catch (Exception ignored) {
                    return;
                }

                if (message == null)
                    return;

                long ourId = bot.getJda().getSelfUser().getIdLong();
                Map<ReactionEmote, Integer> resultTable = message.getReactions().stream()
                        .map(r -> ImmutablePair.of(r, (int) r.getUsers()
                                .complete()
                                .stream()
                                .filter(u -> u.getIdLong() != ourId)
                                .count()))
                        .sorted(Collections.reverseOrder(Comparator.comparing(ImmutablePair<MessageReaction, Integer>::getRight)))
                        .collect(Collectors.toMap(
                                e -> e.getLeft().getEmote(),
                                ImmutablePair::getRight,
                                (k, v) -> { throw new IllegalStateException("Duplicate key " + k); },
                                LinkedHashMap::new
                        ));

                ReactionEmote winnerKey = Collections.max(resultTable.entrySet(), Map.Entry.comparingByValue()).getKey();
                String winner = winnerKey.getEmote() == null ? winnerKey.getName() : winnerKey.getEmote().getAsMention();

                List<String> orderedResultList = resultTable.entrySet().stream()
                        .map(e -> {
                            final ReactionEmote key = e.getKey();
                            final Integer value = e.getValue();
                            final String userKey = key.getEmote() == null ? key.getName() : key.getEmote().getAsMention();

                            return userKey + ": " + value + " vote" + (value == 1 ? '\00' : 's');
                        })
                        .collect(Collectors.toList());

                EmbedBuilder emb = new EmbedBuilder(message.getEmbeds().get(0))
                        .addField("Winner", winner, false);
                emb.getDescriptionBuilder().replace(emb.getDescriptionBuilder().indexOf("**✅ Go ahead and vote!**"),
                        emb.getDescriptionBuilder().length(), "**❌ Poll ended.**");

                message.editMessage(emb.build()).queue();
                channel.sendMessage("**Poll ended!\n" +
                        "Winner: " + winner + "\n\n" +
                        "Full Results:**\n" + String.join("\n", orderedResultList)).queue();
            } catch (Exception e) {
                logger.error("Poll: error", e);
            } finally {
                try {
                    pollDao.delete(poll);
                } catch (SQLException e) {}
            }
        }, calculatedTime, TimeUnit.MILLISECONDS);
    }

    @Command(name = "meme", desc = "Generate a custom meme.", usage = "[meme text / [top text] | [bottom text]]")
    public void cmdMeme(Context ctx) {
        if (ctx.rawArgs.length() < 2) {
            ctx.send(Emotes.getFailure() + " I need some text to use!").queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        FormBody.Builder data = new FormBody.Builder();

        int template = -1024;
        String topText = null;
        String bottomText = null;
        for (ImmutablePair<Pattern, Integer> pair: MEME_PATTERNS) {
            Matcher matcher = pair.getLeft().matcher(ctx.rawArgs);
            if (!matcher.find())
                continue;

            try {
                if (matcher.group(1) == null || matcher.group(2) == null)
                    continue;
            } catch (IndexOutOfBoundsException ignored) {
                continue;
            }

            topText = matcher.group(1);
            bottomText = matcher.group(2);
            template = pair.getRight();
            break;
        }

        if (template == -1024)
            template = randomChoice(MEME_PATTERNS).getRight();

        if (topText == null || bottomText == null) {
            if (ctx.rawArgs.indexOf('|') != -1) {
                final int sepIndex = ctx.rawArgs.indexOf('|');

                topText = ctx.rawArgs.substring(0, sepIndex).trim();
                bottomText = ctx.rawArgs.substring(sepIndex + 1).trim();
            } else if (ctx.args.size() == 1 && ctx.rawArgs.length() % 2 != 0) {
                topText = ctx.args.get(0);
                bottomText = " ";
            } else {
                String[] results = ArrayUtils.subarray(StringUtils.split(WordUtils.wrap(StringUtils.replaceChars(
                        ctx.rawArgs, '\n', ' '), ctx.rawArgs.length() / 2,
                        "\n", true, "\\s+"), '\n'),
                        0, 2);

                topText = results[0];
                bottomText = results[1];
            }
        }

        data.add("template_id", str(template));
        try {
            data.add("username", bot.getKeys().getJSONObject("imgflip").getString("username"));
            data.add("password", bot.getKeys().getJSONObject("imgflip").getString("password"));
        } catch (JSONException none) {
            data.add("username", "imgflip_hubot");
            data.add("password", "imgflip_hubot");
        }
        data.add("text0", topText);
        data.add("text1", bottomText);

        Bot.http.newCall(new Request.Builder()
                .post(data.build())
                .url("https://api.imgflip.com/caption_image")
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("Imgflip request errored", e);
                ctx.send(Emotes.getFailure() + " Request failed. `" + e.getMessage() + '`').queue();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject resp = new JSONObject(response.body().string());

                if (resp.optBoolean("success", false)) {
                    ctx.send(new EmbedBuilder()
                            .setColor(randomColor())
                            .setImage(resp.getJSONObject("data").getString("url"))
                            .build()).queue();
                } else {
                    ctx.send(Emotes.getFailure() + " Error: `" + resp.getString("error_message") + '`').queue();
                }
            }
        });
    }

    @Command(name = "urban", desc = "Define something with Urban Dictionary.", aliases = {"define"})
    public void cmdUrban(Context ctx) throws UnsupportedEncodingException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need a term!").queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        Bot.http.newCall(new Request.Builder()
                .get()
                .url("https://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(ctx.rawArgs, "UTF-8"))
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("Urban Dictionary API error", e);
                ctx.send(Emotes.getFailure() + " Request failed.").queue();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONArray results = new JSONObject(response.body().string()).getJSONArray("list");

                if (results.length() < 1) {
                    ctx.send(Emotes.getFailure() + " No definitions found.").queue();
                    return;
                }
                JSONObject word = results.getJSONObject(0);

                EmbedBuilder emb = new EmbedBuilder()
                        .setColor(randomColor())
                        .setTitle(word.getString("word"))
                        .setAuthor("Urban Dictionary", word.getString("permalink"), "https://images.discordapp.net/.eJwFwdsNwyAMAMBdGICHhUPIMpULiCAlGIHzUVXdvXdf9cxLHeoUGeswJreVeGa9hCfVoitzvQqNtnTi25AIpfMuXZaBDSM4G9wWAdA5vxuIAQNCQB9369F7a575pv7KLUnjTvOjR6_q9wdVRCZ_.BorCGmKDHUzN6L0CodSwX7Yv3kg");

                String definition = word.getString("definition");
                if (definition.length() > 0) {
                    for (String page: embedFieldPages(definition)) {
                        emb.addField("Definition", page, false);
                    }
                } else {
                    emb.addField("Definition", "None?!", false);
                }

                String example = word.getString("example");
                if (example.length() > 0) {
                    for (String page: embedFieldPages(example)) {
                        emb.addField("Example", page, false);
                    }
                } else {
                    emb.addField("Example", "None?!", false);
                }

                emb.addField("\uD83D\uDC4D", str(word.getInt("thumbs_up")), true)
                        .addField("\uD83D\uDC4E", str(word.getInt("thumbs_down")), true);

                ctx.send(emb.build()).queue();
            }
        });
    }

    @Command(name = "rcolor", desc = "Generate a random color.", aliases = {"rc", "randcolor"})
    public void cmdRcolor(Context ctx) {
        final Color color = randomColor();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(color)
                .setTitle("Hex: #" +
                        String.format("%02x%02x%02x", r, g, b) +
                        " | RGB: " +
                        r +
                        ", " +
                        g +
                        ", " +
                        b +
                        " | Integer: " +
                        Math.abs(color.getRGB()));

        ctx.send(embed.build()).queue();
    }

    @Command(name = "charinfo", desc = "Get the Unicode character info for some text.", usage = "[text]")
    public void cmdCharInfo(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need some text!").queue();
            return;
        }

        Paginator pager = new Paginator();
        PrimitiveIterator.OfInt iterator = new UnisafeString(StringUtils.replace(ctx.rawArgs, "\n", "")).chars();

        while (iterator.hasNext()) {
            int codepoint = iterator.nextInt();
            final String fmt = ArrayUtils.contains(CHAR_NO_PREVIEW, codepoint) ||
                    ArrayUtils.contains(DIRECTIONALITY_NO_PREVIEW, Character.getDirectionality(codepoint)) ?
                    "U+%04X %2$s %1$c" : "U+%04X %2$s %1$c (`%1$c`)";

            pager.addLine(String.format(fmt, codepoint, Character.getName(codepoint)));
        }

        String[] pages = pager.getPages();
        if (pages.length > 4) {
            pages = ArrayUtils.subarray(pages, 0, 4);
            ctx.send("Result trimmed to 4 pages.").queue();
        }

        for (String page: pages) {
            ctx.send(page).queue();
        }
    }

    @Command(name = "encode", desc = "Encode some text into Base65536.", usage = "[text]")
    public void cmdEncode(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need some text!").queue();
            return;
        }

        byte[] bytes;
        try {
            bytes = ctx.rawArgs.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            ctx.send(Emotes.getFailure() + " The bot's system doesn't support an essential encoding.").queue();
            return;
        }

        ctx.send("```" + Base65536.encode(bytes) + "```").queue();
    }

    @Command(name = "decode", desc = "Decode Base65536 into regular text.", usage = "[text]")
    public void cmdDecode(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need some text!").queue();
            return;
        }

        byte[] rawOutput;
        try {
            rawOutput = Base65536.decode(ctx.rawArgs);
        } catch (DecoderException e) {
            ctx.send(Emotes.getFailure() + " Error: `" + e.getMessage() + '`').queue();
            return;
        }

        String decoded;
        try {
            decoded = new String(rawOutput, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ctx.send(Emotes.getFailure() + " The bot's system doesn't support an essential encoding.").queue();
            return;
        }

        ctx.send("```" + decoded + "```").queue();
    }

    @Cooldown(scope = BucketType.USER, delay = 5)
    @Command(name = "minecraft", desc = "Get information about a Minecraft server.",
            usage = "[server address]", aliases = {"mc", "mcserver"}, thread = true)
    public void cmdMineServer(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need a server address.").queue();
            return;
        }

        int port = 25565;
        String[] portSplit = StringUtils.split(ctx.rawArgs, ':');
        String server = StringUtils.replace(portSplit[0], "/", "");

        if (portSplit.length > 1) {
            try {
                port = Integer.parseInt(portSplit[1]);
            } catch (NumberFormatException ignored) {}
        }
        if (server.indexOf((int)'.') == -1 || ctx.rawArgs.indexOf((int)' ') != -1) {
            ctx.send(Emotes.getFailure() + " Invalid address.").queue();
            return;
        }

        ctx.channel.sendTyping().queue();

        JSONObject data;
        try {
            data = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(server).setPort(port).setTimeout(5000));
        } catch (IOException e) {
            logger.error("Error connecting to Minecraft server:", e);
            ctx.send(Emotes.getFailure() + " A network error occurred.").queue();
            return;
        }

        String desc;
        String serverType = "Vanilla";
        Object dataDesc = data.get("description");
        JSONObject dataPlayers = data.getJSONObject("players");

        if (dataDesc instanceof JSONObject) {
            JSONObject descObj = (JSONObject) dataDesc;

            if (descObj.optString("text", "").length() > 0) {
                desc = descObj.getString("text");
            } else {
                desc = MinecraftUtil.decodeJsonText(descObj);
            }
        } else if (dataDesc instanceof String) {
            desc = (String) dataDesc;
        } else {
            desc = dataDesc.toString();
        }
        desc = MC_COLOR_PATTERN.matcher(desc).replaceAll("");

        EmbedBuilder emb = new EmbedBuilder()
                .setTitle(server + ':' + port)
                .setDescription(desc)
                .setColor(randomColor())
                .setFooter(getEffectiveName(ctx), ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .addField("Players", dataPlayers.getInt("online") + "/" + dataPlayers.getInt("max"), true);

        if (val(dataPlayers.optJSONArray("sample")).or(EMPTY_JSON_ARRAY).length() > 0) {
            String content = MC_COLOR_PATTERN.matcher(
                    smartJoin(StreamUtils.asStream(dataPlayers.getJSONArray("sample").iterator())
                            .map(elem ->
                                    ((JSONObject) elem).getString("name")
                            )
                            .collect(Collectors.toList()))).replaceAll("");

            if (content.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
                emb.addField("Players Online", content, true);
            } else {
                for (String page: embedFieldPages(content)) {
                    emb.addField("Players Online", page, true);
                }
            }
        }

        emb.addField("Version", MC_COLOR_PATTERN.matcher(data.getJSONObject("version").getString("name"))
                .replaceAll(""), true);
        emb.addField("Protocol Version", str(data.getJSONObject("version").getInt("protocol")), true);

        if (data.has("modinfo")) {
            JSONObject modinfo = data.getJSONObject("modinfo");
            if (modinfo.has("modList") && modinfo.getJSONArray("modList").length() > 0) {
                String content = smartJoin(StreamUtils.asStream(modinfo.getJSONArray("modList").iterator())
                        .map(elem -> {
                            JSONObject mod = (JSONObject) elem;

                            return WordUtils.capitalize(mod.getString("modid")) +
                                    ' ' +
                                    mod.getString("version");
                        })
                        .collect(Collectors.toList()));

                if (content.length() <= 1024) {
                    emb.addField("Mods", content, true);
                } else {
                    for (String page: embedFieldPages(content)) {
                        emb.addField("Mods", page, true);
                    }
                }
            }

            if (modinfo.has("type")) {
                String type = modinfo.getString("type");

                if (type.equalsIgnoreCase("fml")) {
                    serverType = "Forge / FML";
                } else {
                    serverType = WordUtils.capitalize(type);
                }
            }
        }

        emb.addField("Server Type", serverType, true);
        emb.addField("Ping", format("{0,number}ms", data.getInt("ping_millis")), true);

        ctx.send(emb.build()).queue();
    }

    @Cooldown(scope = BucketType.USER, delay = 20)
    @Command(name = "contact", desc = "Contact the bot owner with a message.", usage = "[message]")
    public void cmdContact(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need a message!").queue();
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(getTag(ctx.author), ctx.author.getEffectiveAvatarUrl(), ctx.author.getEffectiveAvatarUrl())
                .setDescription(ctx.rawArgs)
                .addField("Message ID", ctx.message.getId(), true)
                .addField("Author ID", ctx.author.getId(), true)
                .addField("Channel", format("**{0}**\nID: `{1}`", ctx.channel.getName(), ctx.channel.getId()), true)
                .addField("Sent via PM?", ctx.channel instanceof PrivateChannel ? "Yes" : "No", true)
                .addField("Time", ctx.message.getCreationTime().toString(), true)
                .addField("Timestamp", str(ctx.message.getCreationTime().toEpochSecond()), true)
                .addField("Contains Mention?", ctx.message.getMentionedUsers().size() > 0 ? "Yes" : "No", true);

        if (ctx.guild != null) {
            GuildImpl guild = (GuildImpl) ctx.guild;
            emb.addField("Guild", new StringBuilder("**")
                    .append(guild.getName())
                    .append("**\nID: `")
                    .append(guild.getId())
                    .append("`\nMembers: ")
                    .append(guild.getMembersMap().size())
                    .toString(), true);
        }

        PrivateChannel ownerChannel = bot.owner.openPrivateChannel().complete(); // one-time and fast enough
        ownerChannel.sendMessage(new MessageBuilder()
                .append("📧 New message.")
                .setEmbed(emb.build())
                .build()).queue();

        ctx.send(Emotes.getSuccess() + " Message sent.").queue();
    }

    @Command(name = "rprofile", desc = "Generate a random person.", aliases = {"rperson"})
    public void cmdRprofile(Context ctx) {
        Person person = fairy.person();

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(person.getFirstName(), null, "https://discordapp.com/assets/" + randomChoice(UserImpl.DefaultAvatar.values()).toString() + ".png")
                .addField("Full Name", person.getFullName(), false)
                .addField("Age", str(person.getAge()), true)
                .addField("Email", person.getEmail(), true)
                .addField("Date of Birth", person.getDateOfBirth().toString("MM/dd/yyyy"), true)
                .addField("Phone Number", person.getTelephoneNumber(), true)
                .addField("Address", person.getAddress().toString(), true)
                .addField("Company", person.getCompany().getName(), true)
                .addField("Username", person.getUsername(), true)
                .addField("Gender", WordUtils.capitalizeFully(person.getSex().name()), true)
                .addField("Passport Number", person.getPassportNumber(), true)
                .setFooter("Fake profiles FTW!", null)
                .setTimestamp(Instant.now());

        ctx.send(emb.build()).queue();
    }

    private byte[] encodeBarcode(String text, BarcodeFormat format, int size)
            throws WriterException, IOException {
        BitMatrix matrix = qrWriter.encode(text, format, size, size, qrHintMap);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", stream);

        return stream.toByteArray();
    }

    @Command(name = "qrcode", desc = "Generate a QR code.", aliases = {"qr"}, thread = true)
    public void cmdQrcode(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need some text!").queue();
            return;
        }

        byte[] data;
        try {
            data = encodeBarcode(ctx.rawArgs, BarcodeFormat.QR_CODE, 256);
        } catch (WriterException|IOException e) {
            logger.error("QR code error", e);
            ctx.send(Emotes.getFailure() + " An error occurred. Text too long?").queue();
            return;
        }

        ctx.channel.sendFile(data, "qrcode.png", null).queue();
    }

    @Command(name = "permissions", desc = "See your permissions here.", aliases = {"perms"})
    public void cmdPerms(Context ctx) {
        List<Permission> perms = ctx.guild == null ?
                Permission.getPermissions(379968) :
                ctx.member.getPermissions((Channel) ctx.channel);

        List<String> permList = perms.stream()
                .map(perm -> "**" + perm.getName() + "**")
                .collect(Collectors.toList());

        ctx.send("You have " + smartJoin(permList) + " here.").queue();
    }

    @Command(name = "xkcd", desc = "All that xkcd goodness!", thread = true)
    public void cmdXkcd(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send("🤔 **You need to specify what to get!**\n" +
                    "The following are valid:\n" +
                    "    \u2022 `latest`\n" +
                    "    \u2022 `random`\n" +
                    "    \u2022 `number [comic number]`\n" +
                    "    \u2022 `[comic number]`").queue();
            return;
        }

        String first = ctx.args.get(0);
        String second = "";
        try {
            second = ctx.args.get(1);
        } catch (IndexOutOfBoundsException e) {}

        String comicTitle;
        String comicUrl;
        String comicDesc;
        int comicNum;

        if (first.equalsIgnoreCase("latest")) {
            ctx.channel.sendTyping().queue();

            try {
                comicNum = new JSONObject(Bot.http.newCall(new Request.Builder()
                        .get()
                        .url("https://xkcd.com/info.0.json")
                        .build()).execute().body().string())
                        .getInt("num");
            } catch (IOException e) {
                logger.error("xkcd > latest: http error", e);
                ctx.send(Emotes.getFailure() + " An error occurred.").queue();
                return;
            }
        } else if (first.equalsIgnoreCase("random")) {
            ctx.channel.sendTyping().queue();

            try {
                comicNum = randint(1, new JSONObject(Bot.http.newCall(new Request.Builder()
                        .get()
                        .url("https://xkcd.com/info.0.json")
                        .build()).execute().body().string())
                        .getInt("num") + 1);
            } catch (IOException e) {
                logger.error("xkcd > random: http error", e);
                ctx.send(Emotes.getFailure() + " An error occurred.").queue();
                return;
            }
        } else if (((first.equalsIgnoreCase("number") || first.equalsIgnoreCase("num")) &&
                Strings.is4Digits(second)) || Strings.is4Digits(first)) {
            ctx.channel.sendTyping().queue();

            try {
                int max = new JSONObject(Bot.http.newCall(new Request.Builder()
                        .get()
                        .url("https://xkcd.com/info.0.json")
                        .build()).execute().body().string())
                        .getInt("num");
                int requested;
                try {
                    requested = Integer.parseInt(first);
                } catch (NumberFormatException e) {
                    requested = Integer.parseInt(second);
                }

                if (requested > 0 && requested <= max) {
                    comicNum = requested;
                } else {
                    ctx.send(Emotes.getFailure() + " Invalid comic. The latest is " + max + '.').queue();
                    return;
                }
            } catch (IOException e) {
                logger.error("xkcd > random: http error", e);
                ctx.send(Emotes.getFailure() + " An error occurred.").queue();
                return;
            }
        } else {
            ctx.send("🤔 **Invalid comic!**\n" +
                    "The following are valid:\n" +
                    "    \u2022 `latest`\n" +
                    "    \u2022 `random`\n" +
                    "    \u2022 `number [comic number]`\n" +
                    "    \u2022 `[comic number]`").queue();
            return;
        }

        try {
            JSONObject resp = new JSONObject(Bot.http.newCall(new Request.Builder()
                    .get()
                    .url("http://www.xkcd.com/" + comicNum + "/info.0.json")
                    .build()).execute().body().string());

            comicTitle = resp.getString("safe_title");
            comicDesc = resp.getString("alt");
            comicUrl = resp.getString("img");
        } catch (IOException e) {
            logger.error("xkcd: http error", e);
            ctx.send(Emotes.getFailure() + " An error occurred.").queue();
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(comicTitle, "https://xkcd.com/" + comicNum, null)
                .setImage(comicUrl)
                .setFooter(comicDesc, null);

        ctx.send(emb.build()).queue();
    }

    @Command(name = "zwsp", desc = "Get a zero width space.", aliases = {"u200b", "200b"})
    public void cmdZwsp(Context ctx) {
        ctx.send("\u200b").queue();
    }

    @Command(name = "b64encode", desc = "Encode text into Base64.")
    public void cmdB64encode(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need some text!").queue();
            return;
        }

        ctx.send("```" + Base64.getEncoder().encodeToString(ctx.rawArgs.getBytes()) + "```").queue();
    }

    @Command(name = "b64decode", desc = "Decode Base64 into text.")
    public void cmdB64decode(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need some text!").queue();
            return;
        }

        try {
            ctx.send("```" + new String(Base64.getDecoder().decode(ctx.rawArgs)) + "```").queue();
        } catch (IllegalArgumentException e) {
            ctx.send(Emotes.getFailure() + " An error occurred: `" + e.getMessage() + "`").queue();
        }
    }

    @Command(name = "ipinfo", desc = "Get information about an IP or domain.", aliases = {"ip"}, thread = true)
    public void cmdIpInfo(Context ctx) throws Throwable {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need an IP or domain!").queue();
            return;
        } else if (!Strings.isIPorDomain(ctx.rawArgs)) {
            ctx.send(Emotes.getFailure() + " Invalid domain, IPV4, or IPV6 address!").queue();
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
                ctx.send(Emotes.getFailure() + " Request failed.").queue();
            } else {
                throw e.getCause();
            }
        }
    }

    @Command(name = "mcskin", desc = "Get someone's Minecraft skin.", usage = "[username]")
    public void cmdMcskin(Context ctx) {
        if (!Strings.isMinecraftName(ctx.rawArgs)) {
            ctx.send(Emotes.getFailure() + " I need a valid username!").queue();
            return;
        }
        final String name = ctx.rawArgs;

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(name + "'s skin", null, "https://use.gameapis.net/mc/images/avatar/" + name + "/150/true")
                .setImage("https://use.gameapis.net/mc/images/skin/" + name + "/150/true")
                .build()).queue();
    }

    @Command(name = "mchead", desc = "Get someone's Minecraft head.", usage = "[username]")
    public void cmdMchead(Context ctx) {
        if (!Strings.isMinecraftName(ctx.rawArgs)) {
            ctx.send(Emotes.getFailure() + " I need a valid username!").queue();
            return;
        }
        final String name = ctx.rawArgs;

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(name + "'s head", null, "https://use.gameapis.net/mc/images/avatar/" + name + "/150/true")
                .setImage("https://use.gameapis.net/mc/images/avatar/" + name + "/150/true")
                .build()).queue();
    }

    @Command(name = "mcstatus", desc = "Check the status of all the official Minecraft services.",
            aliases = {"mc_status", "minestatus", "mine_status", "minecraft_status"})
    public void cmdMcstatus(Context ctx) {

        Bot.http.newCall(new Request.Builder()
                .get()
                .url("https://use.gameapis.net/mc/extra/status")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("mcapi status API error", e);
                ctx.send(Emotes.getFailure() + " Failed to check Minecraft services.").queue();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                EmbedBuilder emb = new EmbedBuilder()
                        .setAuthor("Minecraft Status", null,
                                "https://minecraft.gamepedia.com/media/minecraft.gamepedia.com/c/c5/Grass.png")
                        .setColor(ThreadLocalRandom.current().nextInt(2) == 1 ? MC_GREEN : MC_BROWN)
                        .setTimestamp(Instant.now());

                JSONObject json = new JSONObject(response.body().string());
                for (String key: json.keySet()) {
                    String status = json.getJSONObject(key).getString("status");
                    emb.addField(key, status.equals("Online") ? "✅" : "❌", false);
                }

                ctx.send(emb.build()).queue();
            }
        });
    }

    @Command(name = "supporters", desc = "Get a list of Patreon supporters.", aliases = {"patrons", "patreon"})
    public void cmdSupporters(Context ctx) {
        if (!(Bot.patreonData.has("rand") && Bot.patreonData.has("always"))) {
            ctx.send(Emotes.getFailure() + " The Patreon data loaded is invalid. Contact the owner.").queue();
            return;
        }
        EmbedBuilder emb = newEmbedWithAuthor(ctx)
                .setColor(randomColor())
                .setDescription("Support me at <https://patreon.com/kdragon>!")
                .setFooter("If you ❤ Goldmine, please become a Patron.", null);
        StringBuilder randBuilder = new StringBuilder();
        StringBuilder alwaysBuilder = new StringBuilder();
        List<Object> randList = Bot.patreonData.getJSONArray("rand").toList();
        Collections.shuffle(randList);

        for (int i = 0; i < randList.size() && i < 10; i++) {
            randBuilder.append("\u2022 ")
                    .append((String) randList.get(i))
                    .append('\n');
        }
        emb.addField("Some Supporters", randBuilder.toString(), false);

        for (Object name: Bot.patreonData.getJSONArray("always")) {
            alwaysBuilder.append("\u2022 ")
                    .append((String) name)
                    .append('\n');
        }
        emb.addField("More Supporters", alwaysBuilder.toString(), false);

        ctx.send(emb.build()).queue();
    }

    @Command(name = "inviteinfo", desc = "Get information about an invite.", usage = "[invite link/code]", aliases = {"iinfo", "invinfo"})
    public void cmdInviteInfo(Context ctx) {
        Matcher matcher = INVITE_PATTERN.matcher(ctx.rawArgs);
        if (!matcher.find()) {
            ctx.send(Emotes.getFailure() + " Invalid or **expired** invite link or code!").queue();
            return;
        }
        String code = matcher.group(1);
        if (code == null)
            code = matcher.group(2);
        ctx.channel.sendTyping().queue();

        Consumer<Invite> processor = invite -> {
            final Invite.Channel channel = invite.getChannel();
            final Invite.Guild guild = invite.getGuild();

            String iconUrl = ctx.jda.getSelfUser().getEffectiveAvatarUrl();
            if (guild.getIconUrl() != null)
                iconUrl = guild.getIconUrl();
            EmbedBuilder emb = new EmbedBuilder()
                    .setColor(randomColor())
                    .setAuthor("Invite to " + guild.getName(), null, iconUrl)
                    .addField("Code", invite.getCode(), true)
                    .addField("Server ID", guild.getId(), true)
                    .addField("Server Creation Time", Date.from(guild.getCreationTime().toInstant()).toString(), true)
                    .addField("Channel", (channel.getType() == ChannelType.TEXT ? '#' : '\00') + channel.getName(), true)
                    .addField("Channel Creation Time", Date.from(channel.getCreationTime().toInstant()).toString(), true)
                    .addField("Channel ID", channel.getId(), true);

            if (invite.getInviter() != null) {
                final User user = invite.getInviter();

                emb.addField("Creator", getTag(user) +
                        " (find out more with `" + ctx.prefix + "user " + user.getId() + "`)", true);
            }

            if (guild.getSplashUrl() != null)
                emb.setImage(guild.getSplashUrl());

            if (invite.isExpanded()) {
                if (invite.getMaxUses() == 0)
                    emb.addField("Uses", str(invite.getUses()), true);
                else
                    emb.addField("Uses", invite.getUses() + " of " + invite.getMaxUses(), true);

                emb.setFooter("Invite created at", null)
                        .setTimestamp(invite.getCreationTime())
                        .addField("Expires",
                                invite.getMaxAge() == 0 ? "Never" :
                                        prettyTime.format(Date.from(invite.getCreationTime()
                                                .plusSeconds(invite.getMaxAge()).toInstant())), true);
            }

            ctx.send(emb.build()).queue();
        };

        Invite.resolve(ctx.jda, code).queue(inv -> {
            inv.expand().queue(processor, ignored -> {
                processor.accept(inv);
            });
        }, err -> {
            logger.warn("Error fetching invite info", err);
            ctx.send(Emotes.getFailure() + " Error fetching invite info!").queue();
        });
    }

    @Command(name = "weather", desc = "Get the weather for a place.", usage = "[city]")
    public void cmdWeather(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need a place to get the weather for!").queue();
            return;
        } else if (!bot.getKeys().has("openweathermap")) {
            ctx.send(Emotes.getFailure() + " My owner hasn't set this feature up!").queue();
            return;
        }

        Bot.http.newCall(new Request.Builder()
                .get()
                .url(Strings.buildQueryUrl("http://api.openweathermap.org/data/2.5/find",
                        "q", ctx.rawArgs,
                        "type", "like",
                        "units", "imperial"))
                .header("X-API-Key", bot.getKeys().getString("openweathermap"))
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                ctx.send(Emotes.getFailure() + " Failed to get weather for that location!").queue();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    onFailure(call, null);
                    return;
                }

                JSONObject root = new JSONObject(response.body().string());

                if (root.optInt("count") > 0 && root.getJSONArray("list").length() > 0) {
                    JSONObject data = root.getJSONArray("list").getJSONObject(0);
                    JSONObject wind = val(data.optJSONObject("wind")).or(Bot.EMPTY_JSON_OBJECT);
                    JSONObject main = val(data.optJSONObject("main")).or(Bot.EMPTY_JSON_OBJECT);
                    JSONObject sys = val(data.optJSONObject("sys")).or(Bot.EMPTY_JSON_OBJECT);
                    JSONObject clouds = val(data.optJSONObject("clouds")).or(Bot.EMPTY_JSON_OBJECT);
                    JSONObject condition = val(data.optJSONArray("weather")).or(Bot.EMPTY_JSON_ARRAY).optJSONObject(0);

                    EmbedBuilder emb = new EmbedBuilder()
                            .setAuthor("Weather for " + data.getString("name"), null,
                                    ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                            .addField("💨 Wind", wind.optDouble("speed") +
                                    " mph (direction: " + wind.optInt("deg") + "°)", true)
                            .addField("💧 Humidity",
                                    str(main.optDouble("humidity")) + '%', true)
                            .addField("🌅 Sunrise Time", sys.optLong("sunrise") == 0L ? SHRUG :
                                    new Date(sys.optLong("sunrise")).toString(), true)
                            .addField("🌇 Sunset Time", sys.optLong("sunset") == 0L ? SHRUG :
                                    new Date(sys.optLong("sunset")).toString(), true)
                            .addField("☀ Today's High", str(main.optDouble("temp_max")) + "°F", true)
                            .addField("❄ Today's Low", str(main.optDouble("temp_min")) + "°F", true)
                            .addField("🌡 Temperature Now", str(main.optDouble("temp")) + "°F", true)
                            .addField("☁ Cloudiness", clouds.optInt("all") + "%", true)
                            .addField("⏬ Pressure", main.optInt("pressure") + " hPa", true)
                            .addField("🏙 Condition", "**" + condition.optString("main", SHRUG) +
                                    "** - " + condition.optString("description", SHRUG), true)
                            .setColor(GraphicsUtils.interpolateColors(Color.BLUE, Color.RED,
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
            }
        });
    }

    @Command(name = "snowtime", desc = "Get the time of a Snowflake ID.", aliases = {"snowflake"},
            usage = "[snowflake]")
    public void cmdSnowtime(Context ctx) {
        long id;
        try {
            if (ctx.args.size() < 1 || (id = MiscUtil.parseSnowflake(ctx.args.get(0))) < 0) {
                ctx.send(Emotes.getFailure() + " Invalid Snowflake ID provided!").queue();
                return;
            }
        } catch (NumberFormatException ignored) {
            ctx.send(Emotes.getFailure() + " Invalid Snowflake ID provided!").queue();
            return;
        }

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor("Snowflake Time:", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setTimestamp(MiscUtil.getCreationTime(id))
                .build()).queue();
    }

    @Command(name = "calculate", desc = "Evaluate a mathematical expression.", aliases = {"calc", "calculator"})
    public void cmdCalculate(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need an expression to evalulate!").queue();
            return;
        }

        Object _result;
        try {
            _result = calcEngine.eval("return calc('" +
                    StringUtils.replace(ctx.rawArgs, "'", "\\\"") + "')");
        } catch (ScriptException e) {
            ctx.send(Emotes.getFailure() + " An error occurred evaluating your expression.\n`" +
                    (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()) + '`').queue();
            return;
        }

        String result = _result instanceof String ? (String) _result : _result.toString();

        if (result.length() < 1)
            result = "\u200b";

        ctx.send("```lua\n" + result + "```").queue();
    }
}
