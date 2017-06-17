package com.khronodragon.bluestone.cogs;

import ch.jamiete.mcping.MinecraftPing;
import ch.jamiete.mcping.MinecraftPingOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.smartJoin;
import static com.khronodragon.bluestone.util.Strings.str;
import static java.text.MessageFormat.format;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilityCog extends Cog {
    private static final Logger logger = LogManager.getLogger(UtilityCog.class);
    private static final Pattern UNICODE_EMOTE_PATTERN = Pattern.compile("([\\u20a0-\\u32ff\\x{1f000}-\\x{1ffff}\\x{fe4e5}-\\x{fe4ee}])");
    private static final Pattern CUSTOM_EMOTE_PATTERN = Pattern.compile("<:[a-z_]+:([0-9]{17,19})>", Pattern.CASE_INSENSITIVE);
    private static final int[] CHAR_NO_PREVIEW = {32, 65279};
    private static final String MC_COLOR_PATTERN = "\\u00a7[4c6e2ab319d5f78lnokmr]";

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

    @Command(name = "user", desc = "Get some info about a user.", usage = "{user}", aliases = {"userinfo", "whois"})
    public void cmdUser(Context ctx) { // TODO: parseUser
        ctx.send("java is terrible so this command is impossible to implement").queue();
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
        Runtime runtime = Runtime.getRuntime();
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
        if (ctx.rawArgs.length() == 0) {
            ctx.send(format("<https://discordapp.com/api/oauth2/authorize?client_id={0}&scope=bot&permissions={1}>",
                    ctx.jda.getSelfUser().getId(), "1609825363")).queue();
        } else {
            if (!ctx.rawArgs.matches("^[0-9]{16,20}$")) {
                ctx.send(":warning: Invalid ID!").queue();
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
        if (ctx.args.size() == 0) {
            ctx.send(":warning: Missing question, emotes, and time (in minutes)!").queue();
            return;
        }

        long pollTime;
        try {
            pollTime = Long.parseUnsignedLong(ctx.args.get(ctx.args.size() - 1));
        } catch (NumberFormatException e) {
            ctx.send(":warning: Invalid time! Time is given as integer minutes.").queue();
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
            ctx.send(":warning: You need at least 2 emotes to poll!").queue();
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
        if (ctx.rawArgs.length() == 0) {
            ctx.send(":warning: I need some text to use!").queue();
            return;
        }

        JSONObject json = new JSONObject();

        int template = 61579;
        // TODO: try matching the regex-id pairs against input

        String topText, bottomText;
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
            json.put("username", bot.getKeys().getAsJsonObject("imgflip").get("username").getAsString());
            json.put("password", bot.getKeys().getAsJsonObject("imgflip").get("password").getAsString());
        } catch (NullPointerException none) {
            json.put("username", "imgflip_hubot");
            json.put("password", "imgflip_hubot");
        }
        json.put("text0", topText);
        json.put("text1", bottomText);

        Unirest.post("https://api.imgflip.com/caption_image")
                .body(json)
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {
                        JSONObject resp = response.getBody().getObject();
                        if (resp.getBoolean("success")) {
                            ctx.send(new EmbedBuilder()
                            .setColor(randomColor())
                            .setImage(resp.getJSONObject("data").getString("url"))
                            .build()).queue();
                        } else {
                            ctx.send(":warning: Error! `" + resp.getString("error_message") + '`').queue();
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        logger.error("Imgflip request errored", e);
                        ctx.send(":x: Request failed. `" + e.getMessage() + '`').queue();
                    }

                    @Override
                    public void cancelled() {
                        ctx.send(":x: Request cancelled.").queue();
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
                        .append(color.getRGB())
                        .toString());

        ctx.send(embed.build()).queue();
    }

    @Command(name = "charinfo", desc = "Get the Unicode info for a character(s).", usage = "[text]")
    public void cmdCharInfo(Context ctx) {
        if (ctx.rawArgs.length() == 0) {
            ctx.send(":warning: I need some text!").queue();
            return;
        }

        Paginator pager = new Paginator();
        PrimitiveIterator.OfInt iterator = new UnisafeString(ctx.rawArgs.replace("\n", "")).chars();

        while (iterator.hasNext()) {
            int codepoint = iterator.nextInt();
            final String fmt = ArrayUtils.contains(CHAR_NO_PREVIEW, codepoint) ? "U+%04x %1$s %1%c" : "U+%04x %1$s %1%c (`%1$c`)";

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
        if (ctx.rawArgs.length() == 0) {
            ctx.send(":warning: I need some text!").queue();
            return;
        }

        byte[] bytes;
        try {
            bytes = ctx.rawArgs.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            ctx.send(":x: The bot's system doesn't support an essential encoding.").queue();
            return;
        }

        ctx.send("```" + Base65536.encode(bytes) + "```");
    }

    @Command(name = "decode", desc = "Decode Base65536 into regular text.", usage = "[text]")
    public void cmdDecode(Context ctx) {
        if (ctx.rawArgs.length() == 0) {
            ctx.send(":warning: I need some text!").queue();
            return;
        }

        byte[] rawOutput;
        try {
            rawOutput = Base65536.decode(ctx.rawArgs);
        } catch (DecoderException e) {
            ctx.send(":warning: Error: `" + e.getMessage() + '`').queue();
            return;
        }

        String decoded;
        try {
            decoded = new String(rawOutput, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ctx.send(":x: The bot's system doesn't support an essential encoding.").queue();
            return;
        }

        ctx.send("```" + decoded + "```").queue();
    }

    @Command(name = "minecraft", desc = "Get information about a Minecraft server.",
            usage = "[server address]", aliases = {"mc", "mcserver"}, thread = true)
    @Cooldown(scope = BucketType.USER, delay = 5)
    public void cmdMineServer(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need a server address.").queue();
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
            ctx.send(":warning: Invalid address.").queue();
            return;
        }

        JsonObject data;
        logger.info("Connecting to Minecraft server {}:{}", server, port);
        try {
            data = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(server).setPort(port).setTimeout(5000));
        } catch (IOException e) {
            logger.error("Error connecting to Minecraft server:", e);
            ctx.send(":x: A network error occurred.").queue();
            return;
        }

        String desc;
        String serverType = "Vanilla";
        JsonElement dataDesc = data.get("description");
        JsonObject dataPlayers = data.getAsJsonObject("players");

        if (dataDesc.isJsonObject()) {
            JsonObject descObj = dataDesc.getAsJsonObject();

            if (descObj.has("text")) {
                if (descObj.get("text").getAsString().length() > 0) {
                    desc = descObj.get("text").getAsString();
                } else {
                    desc = MinecraftUtil.decodeJsonText(descObj);
                }
            } else {
                desc = MinecraftUtil.decodeJsonText(descObj);
            }
        } else if (dataDesc.getAsString() != null){
            desc = dataDesc.getAsString();
        } else {
            desc= dataDesc.toString();
        }
        desc = desc.replaceAll(MC_COLOR_PATTERN, "");

        EmbedBuilder emb = new EmbedBuilder()
                .setTitle(server + ':' + port)
                .setDescription(desc)
                .setColor(randomColor())
                .setFooter(getEffectiveName(ctx), ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .addField("Players", dataPlayers.get("online").getAsInt() + "/" + dataPlayers.get("max").getAsInt(), true);

        if (dataPlayers.get("sample") != null && dataPlayers.get("sample").isJsonArray() && dataPlayers.getAsJsonArray("sample").size() > 0) {
            String content = smartJoin(StreamUtils.asStream(dataPlayers.getAsJsonArray("sample").iterator())
                                        .map(elem ->
                                            elem.getAsJsonObject().get("name").getAsString()
                                        )
                                        .collect(Collectors.toList())).replaceAll(MC_COLOR_PATTERN, "");

            if (content.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
                emb.addField("Players Online", content, true);
            } else {
                for (String page: StringUtils.split(WordUtils.wrap(content, 1024, "||", true, "\\s+"), "||")) {
                    emb.addField("Players Online", page, true);
                }
            }
        }

        emb.addField("Version", data.getAsJsonObject("version").get("name").getAsString().replaceAll(MC_COLOR_PATTERN, ""), true);
        emb.addField("Protocol Version", data.getAsJsonObject("version").get("protocol").getAsString(), true);

        if (data.has("modinfo")) {
            JsonObject modinfo = data.getAsJsonObject("modinfo");
            if (modinfo.has("modList")) {
                if (modinfo.getAsJsonArray("modList").size() > 0) {
                    String content = smartJoin(StreamUtils.asStream(modinfo.getAsJsonArray("modList").iterator())
                            .map(elem -> {
                                JsonObject mod = elem.getAsJsonObject();

                                return WordUtils.capitalize(mod.get("modid").getAsString()) +
                                        ' ' +
                                        mod.get("version").getAsString();
                            })
                            .collect(Collectors.toList()));

                    if (content.length() <= 1024) {
                        emb.addField("Mods", content, true);
                    } else {
                        for (String page: StringUtils.split(WordUtils.wrap(content, 1024, "||", true, "\\s+"), "||")) {
                            emb.addField("Mods", page, true);
                        }
                    }
                }
            }

            if (modinfo.has("type")) {
                String type = modinfo.get("type").getAsString();

                if (type.equalsIgnoreCase("fml")) {
                    serverType = "Forge / FML";
                } else {
                    serverType = WordUtils.capitalize(type);
                }
            }
        }

        emb.addField("Server Type", serverType, true);
        emb.addField("Ping", format("{0,number}ms", data.get("ping_millis").getAsInt()), true);

        ctx.send(emb.build()).queue();
    }

    @Command(name = "contact", desc = "Contact the bot owner with a message.", usage = "[message]")
    @Cooldown(scope = BucketType.USER, delay = 20)
    public void cmdContact(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need a message!").queue();
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor(getTag(ctx.author), ctx.author.getEffectiveAvatarUrl(), ctx.author.getEffectiveAvatarUrl())
                .setDescription(ctx.rawArgs)
                .addField("Message ID", ctx.message.getId(), true)
                .addField("Author ID", ctx.author.getId(), true)
                .addField("Channel", format("**{0}**\nID: `{1,number}`", ctx.channel.getName(), ctx.channel.getId()), true)
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

        ctx.send(":thumbsup: Message sent.").queue();
    }

    /*
    rprofile
    qrcode
    avatar
    ocr
    discrim
    permissions (perms)
    xkcd
      - random
      - latest
      - number
    zwsp
    b64encode
    b64decode
    ipinfo (ip) freegeoip.net
    dial
    urban
    bleach
    nick
    mcskin
    mchead
     */
}
