package com.kdrag0n.bluestone.cogs;

import ch.jamiete.mcping.MinecraftPing;
import ch.jamiete.mcping.MinecraftPingOptions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.dao.Dao;
import com.kdrag0n.bluestone.Context;
import com.kdrag0n.bluestone.sql.ContactBannedUser;
import com.kdrag0n.bluestone.sql.UserFaqRecord;

import io.nayuki.qrcodegen.QrCode;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.utils.MiscUtil;
import okhttp3.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static com.kdrag0n.bluestone.util.Strings.smartJoin;
import static com.kdrag0n.bluestone.util.Strings.str;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UtilityCog extends com.kdrag0n.bluestone.Cog {
    private static final Logger logger = LogManager.getLogger(UtilityCog.class);

    private static final Pattern MC_COLOR_PATTERN = Pattern.compile("\\u00a7[4c6e2ab319d5f78lnokmr]");
    private static final JSONArray EMPTY_JSON_ARRAY = new JSONArray();
    private static final Pattern END_MENTION_PATTERN = Pattern.compile(", [<@&0-9>]*$");

    private final ScriptEngine calcEngine = new ScriptEngineManager().getEngineByName("lua");
    private static final ThreadPoolExecutor calcExecutor = new ThreadPoolExecutor(1, 2, 5, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5), new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Bot Calculation Thread %d")
            .build(), new com.kdrag0n.bluestone.handlers.RejectedExecHandlerImpl("Calculation"));
    private final Dao<ContactBannedUser, Long> contactBanDao;
    private final Dao<UserFaqRecord, Long> userFaqDao;

    public UtilityCog(com.kdrag0n.bluestone.Bot bot) {
        super(bot);

        try (InputStream st = com.kdrag0n.bluestone.Bot.class.getClassLoader().getResourceAsStream("assets/calc.lua")) {
            calcEngine.eval(IOUtils.toString(st, StandardCharsets.UTF_8));
        } catch (IOException|ScriptException e) {
            logger.error("Error evaluating calc.lua for calc command", e);
        }

        contactBanDao = setupDao(ContactBannedUser.class);
        userFaqDao = setupDao(UserFaqRecord.class);
    }

    public String getName() {
        return "Utility";
    }

    public String getDescription() {
        return "Essential utility commands, as well as playful ones.";
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "icon", desc = "Get the current server's icon.", guildOnly = true)
    public void cmdIcon(com.kdrag0n.bluestone.Context ctx) {
        ctx.send(com.kdrag0n.bluestone.util.NullValueWrapper.val(ctx.guild.getIconUrl()).or("There's no icon here!")).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "user", desc = "Get some info about a user.",
            usage = "{user}", aliases = {"userinfo", "whois"}, thread = true)
    public void cmdUser(com.kdrag0n.bluestone.Context ctx) throws UnsupportedEncodingException {
        User user;
        if (com.kdrag0n.bluestone.util.Strings.isMention(ctx.rawArgs) && ctx.message.getMentionedUsers().size() > 0)
            user = ctx.message.getMentionedUsers().get(0);
        else if (com.kdrag0n.bluestone.util.Strings.isID(ctx.rawArgs)) {
            try {
                user = ctx.jda.retrieveUserById(Long.parseUnsignedLong(ctx.rawArgs)).complete();
            } catch (ErrorResponseException ignored) {
                user = null;
            }
        } else if (com.kdrag0n.bluestone.util.Strings.isTag(ctx.rawArgs)) {
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
        } else if (ctx.args.empty)
            user = ctx.author;
        else
            user = null;

        if (user == null) {
            ctx.fail("I need a valid @mention, user ID, or user#discriminator!");
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
            emb.setDescription("User is " + com.kdrag0n.bluestone.Emotes.getBotTag());

        if (ctx.guild != null) {
            Member member = ctx.guild.getMember(user);

            if (member != null) {
                if (member.getNickname() != null)
                    emb.addField("Nickname", member.getNickname(), true);

                String status;
                if (member.getGame() == null)
                    status = com.kdrag0n.bluestone.Emotes.getFullMemberStatus(member);
                else {
                    Game game = member.getGame();

                    if (game.getType() == Game.GameType.STREAMING) {
                        status = com.kdrag0n.bluestone.Emotes.getMemberStatus(member) + "Streaming [**" + game.getName() +
                                "**](" + game.getUrl() + ")";
                    } else {
                        status = com.kdrag0n.bluestone.Emotes.getMemberStatus(member) + " Playing [**" + game.getName() +
                                "**](https://google.com/search?q=" + URLEncoder.encode(game.getName(), "UTF-8") +
                                ')';
                    }
                }

                String roleText = member.getRoles().stream()
                        .map(Role::getAsMention)
                        .collect(Collectors.joining(", "));
                if (roleText.length() > 1024) {
                    roleText = END_MENTION_PATTERN.matcher(roleText.substring(0, 1024))
                            .replaceFirst(", **...too many**");
                } else if (roleText.length() < 1) {
                    roleText = "None";
                }

                emb.setColor(com.kdrag0n.bluestone.util.NullValueWrapper.val(member.getColor()).or(Color.WHITE))
                        .addField("Server Join Time",
                                Date.from(member.getJoinDate().toInstant()).toString(), true)
                        .addField("Status", status, true)
                        .addField("Roles", roleText, true);
            }
        }

        ctx.send(emb.build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "invite", desc = "Generate an invite link for myself or another bot.", aliases = {"addbot", "join"},
            usage = "{bot ID (default: me)}")
    public void cmdInvite(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.send('<' + ctx.jda.asBot().getInviteUrl(com.kdrag0n.bluestone.cogs.CoreCog.PERMS_NEEDED) + '>').queue();
            return;
        }

        if (!com.kdrag0n.bluestone.util.Strings.isID(ctx.rawArgs)) {
            ctx.fail("Invalid ID!");
            return;
        } else if (ctx.rawArgs.equals(ctx.jda.getSelfUser().getId())) {
            ctx.send('<' + ctx.jda.asBot().getInviteUrl(com.kdrag0n.bluestone.cogs.CoreCog.PERMS_NEEDED) + '>').queue();
        }

        ctx.send(String.format(
                "<https://discordapp.com/api/oauth2/authorize?client_id=%s&scope=bot&permissions=3072>",
                ctx.rawArgs)).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "urban", desc = "Define something with Urban Dictionary.", aliases = {"define"})
    public void cmdUrban(com.kdrag0n.bluestone.Context ctx) throws UnsupportedEncodingException {
        if (ctx.args.empty) {
            ctx.fail("I need a term!");
            return;
        }
        ctx.channel.sendTyping().queue();

        com.kdrag0n.bluestone.Bot.http.newCall(new Request.Builder()
                .get()
                .url("https://api.urbandictionary.com/v0/define?term=" + URLEncoder.encode(ctx.rawArgs, "UTF-8"))
                .build()).enqueue(com.kdrag0n.bluestone.Bot.callback(response -> {
            JSONArray results = new JSONObject(response.body().string()).getJSONArray("list");

            if (results.length() < 1) {
                ctx.fail("No definitions found.");
                return;
            }
            JSONObject word = results.getJSONObject(0);

            EmbedBuilder emb = new EmbedBuilder()
                    .setColor(randomColor())
                    .setTitle(word.getString("word"))
                    .setAuthor("Urban Dictionary", word.getString("permalink"), "https://images.discordapp.net/.eJwFwdsNwyAMAMBdGICHhUPIMpULiCAlGIHzUVXdvXdf9cxLHeoUGeswJreVeGa9hCfVoitzvQqNtnTi25AIpfMuXZaBDSM4G9wWAdA5vxuIAQNCQB9369F7a575pv7KLUnjTvOjR6_q9wdVRCZ_.BorCGmKDHUzN6L0CodSwX7Yv3kg");

            String definition = word.getString("definition");
            if (definition.length() > 0) {
                if (definition.length() > 2997) definition = definition.substring(0, 2997) + "...";

                for (String page: embedFieldPages(definition)) {
                    emb.addField("Definition", page, false);
                }
            } else {
                emb.addField("Definition", "None?!", false);
            }

            String example = word.getString("example");
            if (example.length() > 0) {
                if (example.length() > 2997) example = example.substring(0, 2997) + "...";

                for (String page: embedFieldPages(example)) {
                    emb.addField("Example", page, false);
                }
            } else {
                emb.addField("Example", "None?!", false);
            }

            emb.addField("\uD83D\uDC4D", com.kdrag0n.bluestone.util.Strings.str(word.getInt("thumbs_up")), true)
                    .addField("\uD83D\uDC4E", com.kdrag0n.bluestone.util.Strings.str(word.getInt("thumbs_down")), true);

            ctx.send(emb.build()).queue();
        }, e -> {
            logger.error("Urban Dictionary API error", e);
            ctx.fail("Request failed.");
        }));
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "rcolor", desc = "Generate a random color.", aliases = {"rc", "randcolor"})
    public void cmdRcolor(com.kdrag0n.bluestone.Context ctx) {
        final Color color = randomColor();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(color)
                .setTitle("Hex: #" +
                        String.format("%02x%02x%02x", r, g, b) +
                        " | RGB: " + r +
                        ", " + g +
                        ", " + b +
                        " | Integer: " +
                        Math.abs(color.getRGB()));

        ctx.send(embed.build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "encode", desc = "Encode some text into Base65536.", usage = "[text]")
    public void cmdEncode(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need some text!");
            return;
        }

        byte[] bytes;
        try {
            bytes = ctx.rawArgs.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return;
        }

        ctx.send("```" + com.kdrag0n.bluestone.util.Base65536.encode(bytes) + "```").queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "decode", desc = "Decode Base65536 into regular text.", usage = "[text]")
    public void cmdDecode(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need some text!");
            return;
        }

        byte[] rawOutput;
        try {
            rawOutput = com.kdrag0n.bluestone.util.Base65536.decode(ctx.rawArgs);
        } catch (DecoderException e) {
            ctx.send(com.kdrag0n.bluestone.Emotes.getFailure() + " Error: `" + e.getMessage() + '`').queue();
            return;
        }

        String decoded;
        try {
            decoded = new String(rawOutput, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ctx.fail("The bot's system doesn't support an essential encoding.");
            return;
        }

        ctx.send("```" + decoded + "```").queue();
    }

    @com.kdrag0n.bluestone.annotations.Cooldown(scope = com.kdrag0n.bluestone.enums.BucketType.USER, delay = 5)
    @com.kdrag0n.bluestone.annotations.Command(name = "minecraft", desc = "Get information about a Minecraft server.",
            usage = "[server address]", aliases = {"mc", "mcserver"}, thread = true)
    public void cmdMineServer(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need a server address or skin name!");
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

        if (server.indexOf((int)'.') == -1 || ctx.rawArgs.indexOf((int)' ') != -1 || !com.kdrag0n.bluestone.util.Strings.isIPorDomain(ctx.rawArgs)) {
            if (com.kdrag0n.bluestone.util.Strings.isMinecraftName(ctx.rawArgs)) {
                final String name = ctx.rawArgs;

                ctx.send(new EmbedBuilder()
                        .setColor(randomColor())
                        .setAuthor(name + "'s skin", null, "https://use.gameapis.net/mc/images/avatar/" + name + "/150/true")
                        .setFooter("Tip: the " + ctx.invoker + " command can also give server info!", null)
                        .setImage("https://use.gameapis.net/mc/images/skin/" + name + "/150/true")
                        .build()).queue();
            } else {
                ctx.fail("Invalid server address or skin name.");
            }

            return;
        }

        ctx.channel.sendTyping().queue();

        JSONObject data;
        try {
            data = new MinecraftPing().getPing(new MinecraftPingOptions().setHostname(server).setPort(port).setTimeout(5000));
        } catch (IOException e) {
            logger.error("Error connecting to Minecraft server:", e);
            ctx.fail("A network error occurred.");
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
                desc = com.kdrag0n.bluestone.util.MinecraftUtil.decodeJsonText(descObj);
            }
        } else if (dataDesc instanceof String) {
            desc = (String) dataDesc;
        } else {
            desc = dataDesc.toString();
        }
        desc = MC_COLOR_PATTERN.matcher(desc).replaceAll("");

        EmbedBuilder emb = new EmbedBuilder()
                .setTitle(server + ':' + port)
                .setDescription('\u200b' + desc)
                .setColor(randomColor())
                .setFooter(getEffectiveName(ctx), ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .addField("Players", dataPlayers.getInt("online") + "/" + dataPlayers.getInt("max"), true);

        if (com.kdrag0n.bluestone.util.NullValueWrapper.val(dataPlayers.optJSONArray("sample")).or(EMPTY_JSON_ARRAY).length() > 0) {
            String content = MC_COLOR_PATTERN.matcher(
                    com.kdrag0n.bluestone.util.Strings.smartJoin(com.kdrag0n.bluestone.util.StreamUtil.asStream(dataPlayers.getJSONArray("sample").iterator())
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
        emb.addField("Protocol Version", com.kdrag0n.bluestone.util.Strings.str(data.getJSONObject("version").getInt("protocol")), true);

        if (data.has("modinfo")) {
            JSONObject modinfo = data.getJSONObject("modinfo");
            if (modinfo.has("modList") && modinfo.getJSONArray("modList").length() > 0) {
                String content = com.kdrag0n.bluestone.util.Strings.smartJoin(com.kdrag0n.bluestone.util.StreamUtil.asStream(modinfo.getJSONArray("modList").iterator())
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
        emb.addField("Ping", String.format("%dms", data.getInt("ping_millis")), true);
        emb.setFooter("Tip: the " + ctx.invoker + " command can also show skins!", null);

        ctx.send(emb.build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Cooldown(scope = com.kdrag0n.bluestone.enums.BucketType.USER, delay = 20)
    @com.kdrag0n.bluestone.annotations.Command(name = "contact", desc = "Contact the bot owner with a message. Read the FAQ first!", usage = "[message]",
            thread = true)
    public void cmdContact(com.kdrag0n.bluestone.Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 6) {
            ctx.fail("I need a valid message!");
            return;
        } else if (contactBanDao.queryForId(ctx.author.getIdLong()) != null) {
            ctx.fail("You're not allowed to contact the owner!");
            return;
        }

        UserFaqRecord faqRecord;
        if (/*Strings.isQuestion(ctx.rawArgs) &&*/ ((faqRecord = userFaqDao.queryForId(ctx.author.getIdLong())) == null ||
                faqRecord.when.before(new Date(System.currentTimeMillis() - 5184000000L)))) {
            // user hasn't read FAQ yet
            ctx.fail("You haven't read the FAQ yet.\nPlease read the FAQ **before** using `contact`, as it saves you, me, and everyone else a lot of time.\n" +
                    "Link:**\u200b https://khronodragon.com/goldmine/faq \u200b**\n\n" +
                    "Once you have read the FAQ, and it **hasn't** answered your question, simply run this command again to proceed.");
            userFaqDao.create(new UserFaqRecord(ctx.author.getIdLong(), true, new Date()));
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(getTag(ctx.author), ctx.author.getEffectiveAvatarUrl(), ctx.author.getEffectiveAvatarUrl())
                .setDescription(ctx.rawArgs)
                .addField("Message ID", ctx.message.getId(), true)
                .addField("Author ID", ctx.author.getId(), true)
                .addField("Channel", String.format("**%s**\nID: `%s`", ctx.channel.getName(), ctx.channel.getId()), true)
                .addField("Sent via PM?", ctx.channel instanceof PrivateChannel ? "Yes" : "No", true)
                .addField("Time", ctx.message.getCreationTime().toString(), true)
                .addField("Timestamp", com.kdrag0n.bluestone.util.Strings.str(ctx.message.getCreationTime().toEpochSecond()), true)
                .addField("Contains Mention?", ctx.message.getMentionedUsers().size() > 0 ? "Yes" : "No", true);

        if (ctx.guild != null) {
            GuildImpl guild = (GuildImpl) ctx.guild;
            emb.addField("Guild", "**" +
                    guild.getName() +
                    "**\nID: `" +
                    guild.getId() +
                    "`\nMembers: " +
                    guild.getMembersMap().size(), true);
        }

        ctx.jda.getUserById(com.kdrag0n.bluestone.Bot.ownerId).openPrivateChannel().queue(ch -> {
            ch.sendMessage(new MessageBuilder()
                    .append("📧 New message.")
                    .setEmbed(emb.build())
                    .build()).queue();

            ctx.success("Message sent.");
        });
    }

    @com.kdrag0n.bluestone.Perm.Owner
    @com.kdrag0n.bluestone.annotations.Command(name = "contact_ban", desc = "Ban an user from contacting the owner.", usage = "[@mention/user ID]",
            thread = true, aliases = "cb")
    public void cmdContactBan(com.kdrag0n.bluestone.Context ctx) throws SQLException {
        long userId;
        User user;

        if (ctx.message.getMentionedUsers().size() > 0) {
            user = ctx.message.getMentionedUsers().get(0);
            userId = user.getIdLong();
        } else if (com.kdrag0n.bluestone.util.Strings.isID(ctx.rawArgs)) {
            userId = MiscUtil.parseSnowflake(ctx.rawArgs);
            user = ctx.jda.retrieveUserById(userId).complete();
        } else {
            ctx.fail("You must @mention a user or provide their ID!");
            return;
        }

        contactBanDao.createOrUpdate(new ContactBannedUser(userId));
        ctx.send(com.kdrag0n.bluestone.Emotes.getSuccess() + " Successfully banned **" + getTag(user) +
                "** from contacting the owner.").queue();
    }

    @com.kdrag0n.bluestone.Perm.Owner
    @com.kdrag0n.bluestone.annotations.Command(name = "contact_ban_list", desc = "List users banned from contacting the owner.", thread = true,
            aliases = {"cb_list"})
    public void cmdContactBanList(com.kdrag0n.bluestone.Context ctx) throws SQLException {
        String rendered =
                contactBanDao.queryForAll().stream().map(q -> "**" +
                        getTag(ctx.jda.retrieveUserById(q.id).complete()) +
                        "** (`" + q.id + "`)").collect(Collectors.joining("\n    \u2022 "));

        if (rendered.length() < 1) {
            ctx.success("Nobody is banned from contacting the owner.");
        } else {
            ctx.send("The following users are banned from contacting the owner:\n    \u2022 " + rendered).queue();
        }
    }

    @com.kdrag0n.bluestone.Perm.Owner
    @com.kdrag0n.bluestone.annotations.Command(name = "contact_ban_remove", desc = "Allow a banned user from adding quotes.", usage = "[@mention/user ID]",
            thread = true, aliases = {"cb_remove", "cb_del", "cb_rm"})
    public void cmdContactBanRemove(com.kdrag0n.bluestone.Context ctx) throws SQLException {
        long userId;
        User user;

        if (ctx.message.getMentionedUsers().size() > 0) {
            user = ctx.message.getMentionedUsers().get(0);
            userId = user.getIdLong();
        } else if (com.kdrag0n.bluestone.util.Strings.isID(ctx.rawArgs)) {
            userId = MiscUtil.parseSnowflake(ctx.rawArgs);
            user = ctx.jda.retrieveUserById(userId).complete();
        } else {
            ctx.fail("You must @mention a user or provide their ID!");
            return;
        }

        int delN = contactBanDao.deleteById(userId);

        if (delN > 0) {
            ctx.send(com.kdrag0n.bluestone.Emotes.getSuccess() + " Successfully unbanned **" + getTag(user) +
                    "** from contacting the owner.").queue();
        } else {
            ctx.send(com.kdrag0n.bluestone.Emotes.getFailure() + " **" + getTag(user) + "** isn't banned from contacting the owner.")
                    .queue();
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "qrcode", desc = "Generate a QR code.", aliases = {"qr"}, thread = true)
    public void cmdQrcode(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need some text!");
            return;
        }

        byte[] data;
        try {
            QrCode qr = QrCode.encodeText(ctx.rawArgs, QrCode.Ecc.LOW);
            BufferedImage img = qr.toImage(6, 2);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", stream);

            data = stream.toByteArray();
        } catch (IllegalArgumentException|IOException e) {
            logger.error("QR code error", e);
            ctx.fail("An error occurred. Text too long?");
            return;
        }

        ctx.channel.sendFile(data, "qrcode.png", null).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "permissions", desc = "See your permissions here.", aliases = {"perms"})
    public void cmdPerms(com.kdrag0n.bluestone.Context ctx) {
        List<Permission> perms = ctx.guild == null ?
                Permission.getPermissions(379968) :
                ctx.member.getPermissions((Channel) ctx.channel);

        List<String> permList = perms.stream()
                .map(perm -> "**" + perm.getName() + "**")
                .collect(Collectors.toList());

        ctx.send("You have " + com.kdrag0n.bluestone.util.Strings.smartJoin(permList) + " here.").queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "xkcd", desc = "All that xkcd goodness!", thread = true)
    public void cmdXkcd(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.send("🤔 **You need to specify what to get!**\n" +
                    "The following are valid:\n" +
                    "    \u2022 `latest`\n" +
                    "    \u2022 `random`\n" +
                    "    \u2022 `number [comic number]`\n" +
                    "    \u2022 `[comic number]`").queue();
            return;
        }

        String first = ctx.args.get(0);
        String second = ctx.args.get(1);
        if (second == null) second = "";

        String comicTitle;
        String comicUrl;
        String comicDesc;
        int comicNum;

        if (first.equalsIgnoreCase("latest")) {
            ctx.channel.sendTyping().queue();

            try {
                comicNum = new JSONObject(com.kdrag0n.bluestone.Bot.http.newCall(new Request.Builder()
                        .get()
                        .url("https://xkcd.com/info.0.json")
                        .build()).execute().body().string())
                        .getInt("num");
            } catch (IOException e) {
                logger.error("xkcd > latest: http error", e);
                ctx.fail("An error occurred.");
                return;
            }
        } else if (first.equalsIgnoreCase("random")) {
            ctx.channel.sendTyping().queue();

            try {
                comicNum = randint(1, new JSONObject(com.kdrag0n.bluestone.Bot.http.newCall(new Request.Builder()
                        .get()
                        .url("https://xkcd.com/info.0.json")
                        .build()).execute().body().string())
                        .getInt("num") + 1);
            } catch (IOException e) {
                logger.error("xkcd > random: http error", e);
                ctx.fail("An error occurred.");
                return;
            }
        } else if (((first.equalsIgnoreCase("number") || first.equalsIgnoreCase("num")) &&
                com.kdrag0n.bluestone.util.Strings.is4Digits(second)) || com.kdrag0n.bluestone.util.Strings.is4Digits(first)) {
            ctx.channel.sendTyping().queue();

            try {
                int max = new JSONObject(com.kdrag0n.bluestone.Bot.http.newCall(new Request.Builder()
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
                    ctx.send(com.kdrag0n.bluestone.Emotes.getFailure() + " Invalid comic. The latest is " + max + '.').queue();
                    return;
                }
            } catch (IOException e) {
                logger.error("xkcd > random: http error", e);
                ctx.fail("An error occurred.");
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
            JSONObject resp = new JSONObject(com.kdrag0n.bluestone.Bot.http.newCall(new Request.Builder()
                    .get()
                    .url("http://www.xkcd.com/" + comicNum + "/info.0.json")
                    .build()).execute().body().string());

            comicTitle = resp.getString("safe_title");
            comicDesc = resp.getString("alt");
            comicUrl = resp.getString("img");
        } catch (IOException e) {
            logger.error("xkcd: http error", e);
            ctx.fail("An error occurred.");
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(comicTitle, "https://xkcd.com/" + comicNum, null)
                .setImage(comicUrl)
                .setFooter(comicDesc, null);

        ctx.send(emb.build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "zwsp", desc = "Get a zero width space.", aliases = {"u200b", "200b"})
    public void cmdZwsp(com.kdrag0n.bluestone.Context ctx) {
        ctx.send("\u200b").queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "b64encode", desc = "Encode text into Base64.")
    public void cmdB64encode(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need some text!");
            return;
        }

        ctx.send("```" + Base64.getEncoder().encodeToString(ctx.rawArgs.getBytes()) + "```").queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "b64decode", desc = "Decode Base64 into text.")
    public void cmdB64decode(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need some text!");
            return;
        }

        try {
            ctx.send("```" + new String(Base64.getDecoder().decode(ctx.rawArgs)) + "```").queue();
        } catch (IllegalArgumentException e) {
            ctx.fail("An error occurred: `" + e.getMessage() + "`");
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "mcskin", desc = "Get someone's Minecraft skin.", usage = "[username]")
    public void cmdMcskin(com.kdrag0n.bluestone.Context ctx) {
        if (!com.kdrag0n.bluestone.util.Strings.isMinecraftName(ctx.rawArgs)) {
            ctx.fail("I need a valid username!");
            return;
        }
        final String name = ctx.rawArgs;

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(name + "'s skin", null, "https://use.gameapis.net/mc/images/avatar/" + name + "/150/true")
                .setImage("https://use.gameapis.net/mc/images/skin/" + name + "/150/true")
                .build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "mchead", desc = "Get someone's Minecraft head.", usage = "[username]")
    public void cmdMchead(com.kdrag0n.bluestone.Context ctx) {
        if (!com.kdrag0n.bluestone.util.Strings.isMinecraftName(ctx.rawArgs)) {
            ctx.fail("I need a valid username!");
            return;
        }
        final String name = ctx.rawArgs;

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(name + "'s head", null, "https://use.gameapis.net/mc/images/avatar/" + name + "/150/true")
                .setImage("https://use.gameapis.net/mc/images/avatar/" + name + "/150/true")
                .build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "supporters", desc = "Get a list of Patreon supporters.",
            aliases = {"patrons", "patreon", "donate", "givemoney"})
    public void cmdSupporters(com.kdrag0n.bluestone.Context ctx) {
        if (!(com.kdrag0n.bluestone.Bot.patreonData.has("rand") && com.kdrag0n.bluestone.Bot.patreonData.has("always"))) {
            ctx.fail("The Patreon data loaded is invalid. Contact the owner.");
            return;
        }
        EmbedBuilder emb = newEmbedWithAuthor(ctx)
                .setColor(com.kdrag0n.bluestone.util.NullValueWrapper.val(ctx.guild.getSelfMember().getColor()).or(com.kdrag0n.bluestone.Cog::randomColor))
                .setDescription("Support me at**\u200b <https://patreon.com/kdragon>\u200b**!")
                .setFooter("If you ❤ " + com.kdrag0n.bluestone.Bot.NAME + ", please become a Patron.", null);
        StringBuilder builder = new StringBuilder(120).append('\u200b');
        List<Object> randList = com.kdrag0n.bluestone.Bot.patreonData.getJSONArray("rand").toList();
        Collections.shuffle(randList);

        for (int i = 0; i < randList.size() && i < 10; i++) {
            builder.append("    \u2022 ")
                    .append((String) randList.get(i))
                    .append('\n');
        }

        for (Object name: com.kdrag0n.bluestone.Bot.patreonData.getJSONArray("always")) {
            builder.append("    \u2022 ")
                    .append((String) name)
                    .append('\n');
        }

        if (randList.size() > 10) {
            builder.append("    \u2022 ... and ")
                    .append(com.kdrag0n.bluestone.util.Strings.str(randList.size() - 10))
                    .append(" more!");
        }

        emb.addField("Supporters", builder.toString(), false);
        ctx.send(emb.build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "snowtime", desc = "Get the time of a Snowflake ID.", aliases = {"snowflake"},
            usage = "[snowflake]")
    public void cmdSnowtime(com.kdrag0n.bluestone.Context ctx) {
        long id;
        try {
            if (ctx.args.length < 1 || (id = MiscUtil.parseSnowflake(ctx.args.get(0))) < 0) {
                ctx.fail("Invalid Snowflake ID provided!");
                return;
            }
        } catch (NumberFormatException ignored) {
            ctx.fail("Invalid Snowflake ID provided!");
            return;
        }

        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor("Snowflake Time:", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setTimestamp(MiscUtil.getCreationTime(id))
                .build()).queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "calculate", desc = "Evaluate a mathematical expression.", aliases = {"calc", "calculator"})
    public void cmdCalculate(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need an expression to evaluate!");
            return;
        } else if (ctx.rawArgs.contains("while") || ctx.rawArgs.contains("for")) {
            ctx.fail("Blocked keywords found!");
            return;
        }

        String pCode = StringUtils.replace(ctx.rawArgs, "**", "^");
        int lastNidx = pCode.lastIndexOf(10);
        String code = lastNidx == -1 ? "" : pCode.substring(0, lastNidx);
        String lastLine = lastNidx == -1 ? pCode : pCode.substring(lastNidx + 1);


        if (lastLine.equals("end")) {
            code += "\nend";
            lastLine = "nil";
        }

        Object _result;
        try {
            final String c = code;
            final String l = lastLine;

            FutureTask<Object> task = new FutureTask<>(() -> calcEngine
                    .eval("return calc([[" + c + "]], [[" + l + "]])"));
            calcExecutor.execute(task);
            _result = task.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException|InterruptedException ignored) {
            ctx.fail("Your expression took too long to evaluate!");
            return;
        } catch (ExecutionException _e) {
            Throwable e = _e.getCause();

            if (e instanceof ScriptException) {
                _result = e.getCause().getCause().getMessage();
            } else {
                _result = e.getMessage();
            }
        }

        if (_result == null)
            _result = "nil";

        String result = _result instanceof String ? (String) _result : _result.toString();

        if (result.length() < 1)
            result = "\u200b";

        ctx.send("```lua\n" + result + "```").queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "strikethrough",
            desc = "Apply a strikethrough effect to any text, works anywhere without special formatting.",
            aliases = {"strike", "st", "unistrike"})
    public void cmdStrikethrough(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need text to strikethrough!");
            return;
        }

        StringBuilder result = new StringBuilder(ctx.rawArgs.length() * 2);

        for (int i = 0; i < ctx.rawArgs.length(); i++) {
            result.append(ctx.rawArgs.charAt(i))
                    .append('\u0336');
        }

        ctx.send(result.toString()).queue();
    }
}
