package com.khronodragon.bluestone.cogs;

import ch.jamiete.mcping.MinecraftPing;
import ch.jamiete.mcping.MinecraftPingOptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.Cooldown;
import com.khronodragon.bluestone.enums.BucketType;
import com.khronodragon.bluestone.util.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.impl.UserImpl;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.smartJoin;
import static com.khronodragon.bluestone.util.Strings.str;
import static java.text.MessageFormat.format;

import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilityCog extends Cog {
    private static final Logger logger = LogManager.getLogger(UtilityCog.class);

    private static final Collection<Permission> PERMS_NEEDED = Collections.unmodifiableCollection(Permission.getPermissions(1609825363));
    private static final Pattern UNICODE_EMOTE_PATTERN = Pattern.compile("([\\u20a0-\\u32ff\\x{1f000}-\\x{1ffff}\\x{fe4e5}-\\x{fe4ee}])");
    private static final Pattern CUSTOM_EMOTE_PATTERN = Pattern.compile("<:[a-z_]+:([0-9]{17,19})>", Pattern.CASE_INSENSITIVE);

    private static final int[] CHAR_NO_PREVIEW = {65279};
    private static final byte[] DIRECTIONALITY_NO_PREVIEW = {Character.DIRECTIONALITY_WHITESPACE, Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE};
    private static final String MC_COLOR_PATTERN = "\\u00a7[4c6e2ab319d5f78lnokmr]";
    private static final JSONArray EMPTY_JSON_ARRAY = new JSONArray();

    private static final Fairy fairy = Fairy.create();
    private static final QRCodeWriter qrWriter = new QRCodeWriter();
    private static final Map<EncodeHintType, Object> qrHintMap = new HashMap<EncodeHintType, Object>() {{
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        put(EncodeHintType.CHARACTER_SET, "UTF-8");
    }};

    private static final String NO_USER = Emotes.getFailure() + ' ' + "I need a valid @mention, user ID, or user#discriminator!";
    private static final String SHRUG = "¯\\_(ツ)_/¯";
    private final LoadingCache<String, EmbedBuilder> ipInfoCache = CacheBuilder.newBuilder()
            .maximumSize(36)
            .expireAfterAccess(6, TimeUnit.HOURS)
            .build(new CacheLoader<String, EmbedBuilder>() {
                @Override
                public EmbedBuilder load(String key) throws UnirestException {
                    String uri = "https://freegeoip.net/json/" + key;
                    JSONObject data = Unirest.get(uri).asJson().getBody().getObject();

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
                            .addField("City", val(data.optString("city")).or(SHRUG), true)
                            .addField("ZIP Code", val(data.optString("zip_code")).or(SHRUG), true)
                            .addField("Timezone", val(data.optString("time_zone")).or(SHRUG), true)
                            .addField("Longitude", val(data.optString("longitude")).or(SHRUG), true)
                            .addField("Latitude", val(data.optString("latitude")).or(SHRUG), true)
                            .addField("Metro Code", data.optInt("metro_code") != 0 ?
                                    data.optString("metro_code") :
                                    SHRUG, true);
                }
            });

    public UtilityCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Utility";
    }
    public String getDescription() {
        return "Essential utility commands, as well as playful ones.";
    }

    @Command(name = "icon", desc = "Get the current guild's icon.", guildOnly = true)
    public void cmdIcon(Context ctx) {
        ctx.send(val(ctx.guild.getIconUrl()).or("There's no icon here!")).queue();
    }

    @Command(name = "user", desc = "Get some info about a user.",
            usage = "{user}", aliases = {"userinfo", "whois"}, thread = true)
    public void cmdUser(Context ctx) throws UnsupportedEncodingException {
        User user;
        if (ctx.rawArgs.matches("^<@!?[0-9]{17,20}>$"))
            user = ctx.message.getMentionedUsers().get(0);
        else if (ctx.rawArgs.matches("^[0-9]{17,20}$"))
            user = ctx.jda.retrieveUserById(Long.parseUnsignedLong(ctx.rawArgs)).complete();
        else if (ctx.rawArgs.matches("^.{2,32}#[0-9]{4}$")) {
            Collection<User> users;
            switch (ctx.channel.getType()) {
                case TEXT:
                    users = ctx.guild.getMembers().stream().map(m -> m.getUser()).collect(Collectors.toList());
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

        String author = getTag(user);
        if (user.isBot())
            author += Emotes.getBotTag();

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(author, user.getEffectiveAvatarUrl(), user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .addField("ID", user.getId(), true)
                .addField("Creation Time", Date.from(user.getCreationTime().toInstant()).toString(), true);

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

                    if (game.getType() == Game.GameType.TWITCH) {
                        status = Emotes.getMemberStatus(member) + "Streaming [**" + game.getName() +
                                "**](" + game.getUrl() + ")";
                    } else {
                        status = Emotes.getMemberStatus(member) + " Playing [**" + game.getName() +
                                "**](https://google.com/search?q=" + URLEncoder.encode(game.getName(), "UTF-8") +
                                ')';
                    }
                }

                emb.setColor(member.getColor())
                        .addField("Guild Join Time",
                                Date.from(member.getJoinDate().toInstant()).toString(), true)
                        .addField("Status", status, true)
                        .addField("Roles", member.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.joining(", ")), true);
            }
        }

        ctx.send(emb.build()).queue();
    }

    @Command(name = "guildinfo", desc = "Get loads of info about this guild.", guildOnly = true, aliases = {"ginfo", "guild", "server", "serverinfo", "sinfo"})
    public void cmdGuildInfo(Context ctx) {
        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(ctx.guild.getName(), null,
                        val(ctx.guild.getIconUrl()).or(ctx.jda.getSelfUser().getEffectiveAvatarUrl()))
                .setFooter(ctx.guild.getSelfMember().getEffectiveName(), ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .addField("ID", ctx.guild.getId(), true)
                .addField("Members", str(ctx.guild.getMembers().size()), true)
                .addField("Channels", str(ctx.guild.getTextChannels().size() + ctx.guild.getVoiceChannels().size()), true)
                .addField("Roles", str(ctx.guild.getRoles().size()) ,true)
                .addField("Emotes", str(ctx.guild.getEmotes().size()), true)
                .addField("Region", ctx.guild.getRegion().getName(), true)
                .addField("Owner", ctx.guild.getOwner().getAsMention(), true)
                .addField("Default Channel", ctx.guild.getPublicChannel().getAsMention(), true)
                .addField("Admins Need 2FA?", ctx.guild.getRequiredMFALevel().getKey() == 1 ? "Yes" : "No", true)
                .addField("Content Scan Level", ctx.guild.getExplicitContentLevel().getDescription(), true)
                .addField("Verification Level", WordUtils.capitalize(ctx.guild.getVerificationLevel().name().toLowerCase()
                        .replace('_', ' ')), true)
                .setThumbnail(ctx.guild.getIconUrl());

        ctx.send(emb.build()).queue();
    }

    @Command(name = "info", desc = "Get some info about me.", aliases = {"about", "stats", "statistics"})
    public void cmdInfo(Context ctx) {
        ShardUtil shardUtil = bot.getShardUtil();
        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/")
                .setColor(randomColor())
                .setDescription("Made by **Dragon5232#1841**")
                .addField("Guilds", str(shardUtil.getGuildCount()), true)
                .addField("Uptime", bot.formatUptime(), true)
                .addField("Requests", str(shardUtil.getRequestCount()), true)
                .addField("Threads", str(Thread.activeCount()), true)
                .addField("Memory Used", bot.formatMemory(), true)
                .addField("Users", str(shardUtil.getUserCount()), true)
                .addField("Channels", str(shardUtil.getChannelCount()), true)
                .addField("Commands", str(new HashSet<>(bot.commands.values()).size()), true);

        if (ctx.jda.getSelfUser().getIdLong() == 239775420470394897L) {
            emb.addField("Invite Link", "https://tiny.cc/goldbot", true);
        }

        ctx.send(emb.build()).queue();
    }

    @Command(name = "invite", desc = "Generate an invite link for myself or another bot.", aliases = {"addbot"})
    public void cmdInvite(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send('<' + ctx.jda.asBot().getInviteUrl(PERMS_NEEDED) + '>').queue();
        } else {
            if (!ctx.rawArgs.matches("^[0-9]{16,20}$")) {
                ctx.send(Emotes.getFailure() + ' ' + "Invalid ID!").queue();
                return;
            }

            ctx.send(format("<https://discordapp.com/api/oauth2/authorize?client_id={0}&scope=bot&permissions=3072>",
                    ctx.rawArgs)).queue();
        }
    }

    @Command(name = "home", desc = "Get my \"contact\" info.", aliases = {"website", "web"})
    public void cmdHome(Context ctx) {
        ctx.send("**Author\\'s Website**: <https://khronodragon.com>\n" +
                "**Forums**: <https://forums.khronodragon.com>\n" +
                "**Short Invite Link**: <https://tiny.cc/goldbot>\n" +
                "**Support Guild**: <https://discord.gg/dwykTHc>").queue();
    }

    private Runnable pollTask(final EqualitySet<ReactionEmote> validEmotes, long messageId, final Map<ReactionEmote, Set<User>> pollTable) {
        return () -> {
            while (true) {
                final MessageReactionAddEvent event = bot.waitForReaction(0, ev -> ev.getMessageIdLong() == messageId &&
                        ev.getUser().getIdLong() != bot.getJda().getSelfUser().getIdLong() &&
                        validEmotes.contains(ev.getReactionEmote()));
                if (event == null) break; // Interrupted, probably by poll time ending

                pollTable.get(validEmotes.normalize(event.getReactionEmote())).add(event.getUser());
            }
        };
    }

    @Command(name = "poll", desc = "Start a poll, with reactions.", usage = "[emotes] [question] [time in minutes]", guildOnly = true)
    public void cmdPoll(Context ctx) {
        if (ctx.args.size() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "Missing question, emotes, and time (in minutes)!").queue();
            return;
        }

        long pollTime;
        try {
            pollTime = Long.parseUnsignedLong(ctx.args.get(ctx.args.size() - 1));
        } catch (NumberFormatException e) {
            ctx.send(Emotes.getFailure() + ' ' + "Invalid time! Time is given as integer minutes.").queue();
            return;
        }
        ctx.args.remove(ctx.args.size() - 1);

        String preQuestion = String.join(" ", ctx.args);
        Set<String> unicodeEmotes = RegexUtil.matchStream(UNICODE_EMOTE_PATTERN, preQuestion)
                                        .map(match -> match.group()).collect(Collectors.toSet());
        Set<Emote> customEmotes = RegexUtil.matchStream(CUSTOM_EMOTE_PATTERN, preQuestion)
                                           .map(m -> ctx.guild.getEmoteById(m.group(1)))
                                           .collect(Collectors.toSet());

        if (customEmotes.contains(null)) {
            customEmotes.remove(null);
        } else if (unicodeEmotes.size() + customEmotes.size() < 2) {
            ctx.send(Emotes.getFailure() + ' ' + "You need at least 2 emotes to poll!").queue();
            return;
        }

        final String question = preQuestion.replaceAll(UNICODE_EMOTE_PATTERN.pattern(), "")
                                           .replaceAll("<:[a-zA-Z_]+:[0-9]{17,19}>", "")
                                           .replaceAll("\\s+", " ").trim();

        Map<ReactionEmote, Set<User>> pollTable = new HashMap<>();
        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor(ctx.member.getEffectiveName() + " is polling...", null, ctx.author.getEffectiveAvatarUrl())
                                .setColor(ctx.member.getColor())
                                .setDescription(question)
                                .appendDescription("\n\n")
                                .appendDescription("**⌛ Reactions are being added...**");
        EqualitySet<ReactionEmote> validEmotes = new EqualitySet<>();

        for (ReactionEmote rEmote: Stream.concat(unicodeEmotes.stream().map(s -> new ReactionEmote(s, null, ctx.jda)),
                customEmotes.stream().map(e -> new ReactionEmote(e))).collect(Collectors.toSet())) {
            pollTable.put(rEmote, new HashSet<>());
            validEmotes.add(rEmote);
        }

        ctx.send(embed.build()).queue(msg -> {
            for (String emote: unicodeEmotes) {
                msg.addReaction(emote).queue();
            }
            for (Emote emote: customEmotes) {
                msg.addReaction(emote).queue();
            }

            Thread pollThread = new Thread(pollTask(validEmotes, msg.getIdLong(), pollTable));
            pollThread.setDaemon(true);
            pollThread.setName("Reaction Poll Counter Thread");

            embed.setDescription(question)
                    .appendDescription("\n\n")
                    .appendDescription("**✅ Go ahead and vote!**");

            bot.getScheduledExecutor().schedule(() -> {
                msg.editMessage(embed.build()).queue(newMsg -> {
                    pollThread.start();

                    bot.getScheduledExecutor().schedule(() -> {
                        try {
                            pollThread.interrupt();

                            Map<ReactionEmote, Integer> resultTable = pollTable.entrySet().stream()
                                    .sorted(Collections.reverseOrder(Comparator.comparing(e -> e.getValue().size()))) // reversed() errors
                                    .collect(Collectors.toMap(
                                            entry -> entry.getKey(),
                                            entry -> entry.getValue().size(),
                                            (u, v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); },
                                            LinkedHashMap::new
                                    ));

                            ReactionEmote winnerKey = Collections.max(resultTable.entrySet(), Map.Entry.comparingByValue()).getKey();
                            String winner = winnerKey.getEmote() == null ? winnerKey.getName() : winnerKey.getEmote().getAsMention();

                            List<String> orderedResultList = resultTable.entrySet().stream()
                                    .map(e -> {
                                        final ReactionEmote key = e.getKey();
                                        final Integer value = e.getValue();
                                        final String userKey = key.getEmote() == null ? key.getName() : key.getEmote().getAsMention();
                                        return userKey + ": " + value + " vote" + (value == 1 ? "" : "s");
                                    })
                                    .collect(Collectors.toList());

                            embed.setDescription(question)
                                    .appendDescription("\n\n")
                                    .appendDescription("**❌ Poll ended.**")
                                    .addField("Winner", winner, false);
                            newMsg.editMessage(embed.build()).queue();

                            ctx.send("**Poll** `" + question + "` **ended!\n" +
                                    "Winner: " + winner + "\n\n" +
                                    "Full Results:**\n" + String.join("\n", orderedResultList)).queue();
                        } catch (Throwable e) {
                            logger.error("Poll stage 2: error", e);
                        }
                    }, pollTime, TimeUnit.MINUTES);
                });
            }, (unicodeEmotes.size() + customEmotes.size()) * (int) (ctx.jda.getPing() * 1.8), TimeUnit.MILLISECONDS);
        });
    }

    @Command(name = "meme", desc = "Generate a custom meme.", usage = "[top text] | [bottom text]")
    public void cmdMeme(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need some text to use!").queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        JSONObject json = new JSONObject();

        int template = 61579;
        // TODO: try matching the regex-id pairs against input

        String topText;
        String bottomText;
        if (ctx.rawArgs.contains("|")) {
            final int sepIndex = ctx.rawArgs.indexOf('|');

            topText = ctx.rawArgs.substring(0, sepIndex).trim();
            bottomText = ctx.rawArgs.substring(sepIndex + 1).trim();
        } else {
            String[] results = ArrayUtils.subarray(StringUtils.split(WordUtils.wrap(ctx.rawArgs.replace("\n", " "), ctx.rawArgs.length() / 2, "\n", true, "\\s+"), '\n'), 0, 2);

            topText = results[0];
            bottomText = results[1];
        }

        json.put("template_id", template);
        try {
            json.put("username", bot.getKeys().getJSONObject("imgflip").getString("username"));
            json.put("password", bot.getKeys().getJSONObject("imgflip").getString("password"));
        } catch (JSONException none) {
            json.put("username", "imgflip_hubot");
            json.put("password", "imgflip_hubot");
        }
        json.put("text0", topText);
        json.put("text1", bottomText);
        logger.info("req {}", json);

        Unirest.post("https://api.imgflip.com/caption_image")
                .body(json)
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {
                        JSONObject resp = response.getBody().getObject();

                        logger.info(resp);
                        if (resp.getBoolean("success")) {
                            ctx.send(new EmbedBuilder()
                            .setColor(randomColor())
                            .setImage(resp.getJSONObject("data").getString("url"))
                            .build()).queue();
                        } else {
                            ctx.send(Emotes.getFailure() + ' ' + "Error: `" + resp.getString("error_message") + '`').queue();
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        logger.error("Imgflip request errored", e);
                        ctx.send(Emotes.getFailure() + ' ' + "Request failed. `" + e.getMessage() + '`').queue();
                    }

                    @Override
                    public void cancelled() {
                        ctx.send(Emotes.getFailure() + ' ' + "Request cancelled.").queue();
                    }
                });
    }

    @Command(name = "urban", desc = "Define something with Urban Dictionary.", aliases = {"define"})
    public void cmdUrban(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need a term!").queue();
            return;
        }

        Unirest.get("http://api.urbandictionary.com/v0/define")
                .queryString("term", ctx.rawArgs)
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {
                        JSONArray results = response.getBody().getObject().getJSONArray("list");

                        if (results.length() < 1) {
                            ctx.send(Emotes.getFailure() + ' ' + "No matches found.").queue();
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

                    @Override
                    public void failed(UnirestException e) {
                        logger.error("Urban Dictionary API error", e);
                        ctx.send(Emotes.getFailure() + ' ' + "Request failed.").queue();
                    }

                    @Override
                    public void cancelled() {
                        ctx.send(Emotes.getFailure() + ' ' + "Request cancelled for some reason...").queue();
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
                .setTitle(new StringBuilder()
                        .append("Hex: #")
                        .append(String.format("%02x%02x%02x", r, g, b))
                        .append(" | RGB: ")
                        .append(r)
                        .append(", ")
                        .append(g)
                        .append(", ")
                        .append(b)
                        .append(" | Integer: ")
                        .append(Math.abs(color.getRGB()))
                        .toString());

        ctx.send(embed.build()).queue();
    }

    @Command(name = "charinfo", desc = "Get the Unicode character info for some text.", usage = "[text]")
    public void cmdCharInfo(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need some text!").queue();
            return;
        }

        Paginator pager = new Paginator();
        PrimitiveIterator.OfInt iterator = new UnisafeString(ctx.rawArgs.replace("\n", "")).chars();

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
            ctx.send(Emotes.getFailure() + ' ' + "I need some text!").queue();
            return;
        }

        byte[] bytes;
        try {
            bytes = ctx.rawArgs.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            ctx.send(Emotes.getFailure() + ' ' + "The bot's system doesn't support an essential encoding.").queue();
            return;
        }

        ctx.send("```" + Base65536.encode(bytes) + "```").queue();
    }

    @Command(name = "decode", desc = "Decode Base65536 into regular text.", usage = "[text]")
    public void cmdDecode(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need some text!").queue();
            return;
        }

        byte[] rawOutput;
        try {
            rawOutput = Base65536.decode(ctx.rawArgs);
        } catch (DecoderException e) {
            ctx.send(Emotes.getFailure() + ' ' + "Error: `" + e.getMessage() + '`').queue();
            return;
        }

        String decoded;
        try {
            decoded = new String(rawOutput, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ctx.send(Emotes.getFailure() + ' ' + "The bot's system doesn't support an essential encoding.").queue();
            return;
        }

        ctx.send("```" + decoded + "```").queue();
    }

    @Cooldown(scope = BucketType.USER, delay = 5)
    @Command(name = "minecraft", desc = "Get information about a Minecraft server.",
            usage = "[server address]", aliases = {"mc", "mcserver"}, thread = true)
    public void cmdMineServer(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need a server address.").queue();
            return;
        }

        int port = 25565;
        String[] portSplit = StringUtils.split(ctx.rawArgs, ':');
        String server = portSplit[0].replace("/", "");

        if (portSplit.length > 1) {
            try {
                port = Integer.parseInt(portSplit[1]);
            } catch (NumberFormatException ignored) {}
        }
        if (server.indexOf((int)'.') == -1 || ctx.rawArgs.indexOf((int)' ') != -1) {
            ctx.send(Emotes.getFailure() + ' ' + "Invalid address.").queue();
            return;
        }

        ctx.channel.sendTyping().queue();

        JSONObject data;
        logger.info("Connecting to Minecraft server {}:{}", server, port);
        try {
            data = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(server).setPort(port).setTimeout(5000));
        } catch (IOException e) {
            logger.error("Error connecting to Minecraft server:", e);
            ctx.send(Emotes.getFailure() + ' ' + "A network error occurred.").queue();
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
        desc = desc.replaceAll(MC_COLOR_PATTERN, "");

        EmbedBuilder emb = new EmbedBuilder()
                .setTitle(server + ':' + port)
                .setDescription(desc)
                .setColor(randomColor())
                .setFooter(getEffectiveName(ctx), ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .addField("Players", dataPlayers.getInt("online") + "/" + dataPlayers.getInt("max"), true);

        if (val(dataPlayers.optJSONArray("sample")).or(EMPTY_JSON_ARRAY).length() > 0) {
            String content = smartJoin(StreamUtils.asStream(dataPlayers.getJSONArray("sample").iterator())
                                        .map(elem ->
                                            ((JSONObject) elem).getString("name")
                                        )
                                        .collect(Collectors.toList())).replaceAll(MC_COLOR_PATTERN, "");

            if (content.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
                emb.addField("Players Online", content, true);
            } else {
                for (String page: embedFieldPages(content)) {
                    emb.addField("Players Online", page, true);
                }
            }
        }

        emb.addField("Version", data.getJSONObject("version").getString("name").replaceAll(MC_COLOR_PATTERN, ""), true);
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
            ctx.send(Emotes.getFailure() + ' ' + "I need a message!").queue();
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
            emb.addField("Guild", new StringBuilder("**")
                                    .append(ctx.guild.getName())
                                    .append("**\nID: `")
                                    .append(ctx.guild.getId())
                                    .append("`\nMembers: ")
                                    .append(ctx.guild.getMembers().size())
                                    .toString(), true);
        }

        PrivateChannel ownerChannel = bot.owner.openPrivateChannel().complete(); // one-time and fast enough
        ownerChannel.sendMessage(new MessageBuilder()
                                    .append(":e_mail: New message.")
                                    .setEmbed(emb.build())
                                    .build()).queue();

        ctx.send(Emotes.getSuccess() + ' ' + "Message sent.").queue();
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
                .setFooter("Fake profiles FTW!", null);

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
            ctx.send(Emotes.getFailure() + ' ' + "I need some text!").queue();
        }

        byte[] data;
        try {
            data = encodeBarcode(ctx.rawArgs, BarcodeFormat.QR_CODE, 256);
        } catch (WriterException|IOException e) {
            logger.error("QR code error", e);
            ctx.send(Emotes.getFailure() + ' ' + "An error occurred. Text too long?").queue();
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
            ctx.send(":thinking: **You need to specify what to get!**\n" +
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
                comicNum = Unirest.get("https://xkcd.com/info.0.json")
                        .asJson()
                        .getBody().getObject()
                        .getInt("num");
            } catch (UnirestException e) {
                logger.error("xkcd > latest: http error", e);
                ctx.send(Emotes.getFailure() + ' ' + "An error occurred.").queue();
                return;
            }
        } else if (first.equalsIgnoreCase("random")) {
            ctx.channel.sendTyping().queue();

            try {
                comicNum = randint(1, Unirest.get("https://xkcd.com/info.0.json")
                        .asJson()
                        .getBody().getObject()
                        .getInt("num") + 1);
            } catch (UnirestException e) {
                logger.error("xkcd > random: http error", e);
                ctx.send(Emotes.getFailure() + ' ' + "An error occurred.").queue();
                return;
            }
        } else if ((first.equalsIgnoreCase("number") && second.matches("^[0-9]{1,4}$")) || first.matches("^[0-9]{1,4}$")) {
            ctx.channel.sendTyping().queue();

            try {
                int max = Unirest.get("https://xkcd.com/info.0.json")
                        .asJson()
                        .getBody().getObject()
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
                    ctx.send(Emotes.getFailure() + ' ' + "Invalid comic. The latest is " + max + '.').queue();
                    return;
                }
            } catch (UnirestException e) {
                logger.error("xkcd > random: http error", e);
                ctx.send(Emotes.getFailure() + ' ' + "An error occurred.").queue();
                return;
            }
        } else {
            ctx.send(":thinking: **Invalid comic!**\n" +
                    "The following are valid:\n" +
                    "    \\u2022 `latest`\n" +
                    "    \\u2022 `random`\n" +
                    "    \\u2022 `number [comic number]`\n" +
                    "    \\u2022 `[comic number]`").queue();
            return;
        }

        try {
            JSONObject resp = Unirest.get("http://www.xkcd.com/" + comicNum + "/info.0.json")
                    .asJson()
                    .getBody().getObject();

            comicTitle = resp.getString("safe_title");
            comicDesc = resp.getString("alt");
            comicUrl = resp.getString("img");
        } catch (UnirestException e) {
            logger.error("xkcd: http error", e);
            ctx.send(Emotes.getFailure() + ' ' + "An error occurred.").queue();
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
            ctx.send(Emotes.getFailure() + ' ' + "I need some text!").queue();
            return;
        }

        ctx.send("```" + Base64.getEncoder().encodeToString(ctx.rawArgs.getBytes()) + "```").queue();
    }

    @Command(name = "b64decode", desc = "Decode Base64 into text.")
    public void cmdB64decode(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need some text!").queue();
            return;
        }

        ctx.send("```" + new String(Base64.getDecoder().decode(ctx.rawArgs)) + "```").queue();
    }

    @Command(name = "ipinfo", desc = "Get information about an IP or domain.", aliases = {"ip"}, thread = true)
    public void cmdIpInfo(Context ctx) throws Throwable {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need an IP or domain!").queue();
            return;
        } else if (!ctx.rawArgs.matches("^(?:localhost|[a-zA-Z\\-.]+\\.[a-z]{2,15}|(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|[0-9a-f:]+)$")) {
            ctx.send(Emotes.getFailure() + ' ' + "Invalid domain, IPV4, or IPV6 address!").queue();
            return;
        }

        try {
            ctx.send(ipInfoCache.get(ctx.rawArgs)
                .setAuthor("IP Data", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .build()).queue();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnirestException) {
                logger.error("ipinfo API error", e.getCause());
                ctx.send(Emotes.getFailure() + ' ' + "Request failed.").queue();
            } else {
                throw e.getCause();
            }
        }
    }

    @Command(name = "mcskin", desc = "Get someone's Minecraft skin.", usage = "[username]")
    public void cmdMcskin(Context ctx) {
        if (!ctx.rawArgs.matches("^[a-zA-Z0-9_]{1,32}$")) {
            ctx.send(Emotes.getFailure() + ' ' + "I need a valid username!").queue();
            return;
        }
        final String name = ctx.rawArgs;

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(name + "'s skin", null, "https://mcapi.ca/avatar/" + name + "/150/true")
                .setImage("https://mcapi.ca/skin/" + name + "/150/true")
                .build()).queue();
    }

    @Command(name = "mchead", desc = "Get someone's Minecraft head.", usage = "[username]")
    public void cmdMchead(Context ctx) {
        if (!ctx.rawArgs.matches("^[a-zA-Z0-9_]{1,32}$")) {
            ctx.send(Emotes.getFailure() + ' ' + "I need a valid username!").queue();
            return;
        }
        final String name = ctx.rawArgs;

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(name + "'s head", null, "https://mcapi.ca/avatar/" + name + "/150/true")
                .setImage("https://mcapi.ca/avatar/" + name + "/150/true")
                .build()).queue();
    }
}
