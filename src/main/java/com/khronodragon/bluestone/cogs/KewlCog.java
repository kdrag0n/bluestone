package com.khronodragon.bluestone.cogs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.Cooldown;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.enums.BucketType;
import com.khronodragon.bluestone.enums.ProfileFlags;
import com.khronodragon.bluestone.sql.UserProfile;
import com.khronodragon.bluestone.util.GraphicsUtils;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.user.UserAvatarUpdateEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KewlCog extends Cog {
    private static final Logger logger = LogManager.getLogger(KewlCog.class);
    private static final Pattern DATE_WEEKDAY_PATTERN = Pattern.compile("^The date [0-9 a-zA-Z]+ is not a ([MTWFS][a-z]+), but a ([MTWFS][a-z]+)\\.$");
    private static final Language language = new AmericanEnglish();
    private final JLanguageTool langTool = new JLanguageTool(language);

    private static final int PROFILE_WIDTH = 1600;
    private static final int PROFILE_HEIGHT = 1000;
    private static final String PROFILE_FONT = "Lato";
    private static final String[] PROFILE_QUESTIONS = {"What's your favorite color?",
            "What's your favorite food?",
            "What do you want people to know about you?",
            "What do you like to do?",
            "What are some neat things you've done?",
            "Tell me a little bit more about yourself."};
    private final LoadingCache<User, byte[]> profileCache = CacheBuilder.newBuilder()
            .concurrencyLevel(2)
            .initialCapacity(8)
            .maximumSize(36)
            .expireAfterWrite(12, TimeUnit.HOURS)
            .build(new CacheLoader<User, byte[]>() {
                @Override
                public byte[] load(User user) throws Exception {
                    BufferedImage avatar = ImageIO.read(bot.http.newCall(new Request.Builder().get()
                            .url(user.getEffectiveAvatarUrl() + "?size=256").build()).execute().body().byteStream());

                    BufferedImage bg;
                    File bgFile = new File("data/profiles/bg/" + user.getIdLong() + ".png");
                    if (bgFile.exists())
                        bg = ImageIO.read(bgFile);
                    else
                        bg = ImageIO.read(FunCog.class.getResourceAsStream("/assets/default_profile_bg.png"));

                    // Card image
                    BufferedImage card = new BufferedImage(PROFILE_WIDTH, PROFILE_HEIGHT, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = card.createGraphics();

                    // Everything here is layered
                    g2d.drawImage(bg, 0, 0, null); // user background

                    // Info box top
                    g2d.setColor(new Color(255, 255, 255, 224));
                    g2d.fillRoundRect(400, 120, 1080, 262, 24, 24);

                    g2d.setColor(new Color(255, 255, 255, 255));
                    g2d.fillRoundRect(400, 100, 1080, 168, 24, 24);

                    // Avatar box
                    g2d.setColor(new Color(80, 80, 80, 255));
                    g2d.fillRoundRect(118, 118, 268, 268, 8, 8);
                    g2d.drawImage(avatar, 124, 124, 256, 256, null);

                    // Font rendering hints
                    g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                            RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                    // Profile info
                    g2d.setColor(new Color(74, 144, 226, 255));
                    g2d.drawString(fstr(user.getName(), new Font(PROFILE_FONT, Font.BOLD, 64)), 420, 192);
                    g2d.drawString(fstr('@' + getTag(user),
                            new Font(PROFILE_FONT, Font.PLAIN, 36)), 420, 248);

                    // Flags
                    TIntList flags = ProfileFlags.getFlags(bot, user);
                    TIntIterator iterator = flags.iterator();
                    int flagI = 0;

                    while (iterator.hasNext()) {
                        int flag = iterator.next();
                        Class<FunCog> cl = FunCog.class;
                        int startx = (540 - (60 * flags.size())) / 2;
                        InputStream iconStream;

                        switch (flag) {
                            case ProfileFlags.BOT_OWNER:
                                iconStream = cl.getResourceAsStream("/assets/owner.png");
                                break;
                            case ProfileFlags.BOT_ADMIN:
                                iconStream = cl.getResourceAsStream("/assets/key.png");
                                break;
                            case ProfileFlags.PATREON_SUPPORTER:
                                iconStream = cl.getResourceAsStream("/assets/patreon.png");
                                break;
                            default:
                                iconStream = cl.getResourceAsStream("/assets/unknown.png");
                                break;
                        }

                        g2d.drawImage(ImageIO.read(iconStream), 674 + startx + (60 * flagI), 292, null);

                        flagI++;
                    }

                    // Info box bottom
                    g2d.setColor(new Color(255, 255, 255, 218));
                    g2d.fillRoundRect(120, 400, 1360, 500, 32, 32);

                    // render text
                    g2d.setColor(new Color(74, 144, 226, 255));
                    UserProfile profile = profileDao.queryForId(user.getIdLong());
                    if (profile != null) {
                        try {
                            JSONArray pairs = new JSONArray(profile.getQuestionValues());
                            g2d.setFont(new Font(PROFILE_FONT, Font.PLAIN, 24));

                            for (int i = 0; i < pairs.length(); i++) {
                                JSONArray pairData = pairs.getJSONArray(i);
                                int x = i < 5 ? 136 : 850;
                                int iMinusN = i < 5 ? 1 : 6;

                                drawMLString(g2d, "[B]" + WordUtils.wrap(pairData.getString(0),
                                        50, "\n", true) + "[/B]\n" +
                                                WordUtils.wrap(pairData.getString(1), 55,
                                                        "\n", true),
                                        x, 504 + ((i - iMinusN) * 96));
                            }
                        } catch (Throwable e) {
                            logger.error("Error drawing user profile questions", e);
                            g2d.setFont(new Font(PROFILE_FONT, Font.BOLD, 84));
                            g2d.setColor(new Color(244, 10, 1, 255));
                            drawMLString(g2d, "An error occurred rendering\nor loading this section!",
                                    160, 440);
                        }
                    } else {
                        g2d.setFont(new Font(PROFILE_FONT, Font.BOLD, 80));
                        drawMLString(g2d,
                                "This user hasn't set up their\nprofile yet!\n(╯°□°）╯︵ ┻━─┬\uFEFF ノ( ゜-゜ノ)",
                                160, 440);
                    }


                    g2d.dispose();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    ImageIO.write(card, "png", stream);

                    return stream.toByteArray();
                }
            });
    private static final TLongSet profileSetupSessions = new TLongHashSet();
    private Dao<UserProfile, Long> profileDao;

    public KewlCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), UserProfile.class);
        } catch (SQLException e) {
            logger.warn("Failed to create profile table!", e);
        }

        try {
            profileDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), UserProfile.class);
        } catch (SQLException e) {
            logger.warn("Failed to create profile DAO!", e);
        }
    }

    public String getName() {
        return "Kewl";
    }

    @Override
    public String getCosmeticName() {
        return "Kewl Stuff";
    }

    public String getDescription() {
        return "All the kewl extensions belong here.";
    }

    @Cooldown(scope = BucketType.USER, delay = 5)
    @Command(name = "correct", desc = "Correct spelling in some text.", thread = true)
    public void cmdSpellcheck(Context ctx) throws IOException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need something to correct!").queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        final String text = ctx.rawArgs;
        StringBuilder result = new StringBuilder(text);

        List<RuleMatch> matches;
        synchronized (langTool) {
            matches = langTool.check(text);
        }
        Collections.reverse(matches);

        for (RuleMatch match: matches) {
            if (match.getSuggestedReplacements().size() > 0) {
                result.replace(match.getFromPos(), match.getToPos(), match.getSuggestedReplacements().get(0));
            } else if (match.getRule().getId().equals("DATE_WEEKDAY")) {
                Matcher m = DATE_WEEKDAY_PATTERN.matcher(match.getMessage());
                if (!m.find()) continue;

                String wrongWeekday = m.group(1);
                String correctWeekday = m.group(2);

                result.replace(match.getFromPos(), match.getToPos(),
                        result.substring(match.getFromPos(), match.getToPos())
                                .replace(wrongWeekday, correctWeekday));
            }
        }
        String finalResult = result.toString().replace(".M..", "M.");

        ctx.send("Result: `" + finalResult + "`").queue();
    }

    @Command(name = "profile", desc = "Display a user's profile.", usage = "[user / \"setup\" / \"bg\"]", thread = true)
    public void cmdProfile(Context ctx) throws SQLException, IOException {
        User user;
        if (ctx.rawArgs.matches("^<@!?[0-9]{17,20}>$") && ctx.message.getMentionedUsers().size() > 0)
            user = ctx.message.getMentionedUsers().get(0);
        else if (ctx.rawArgs.matches("^[0-9]{17,20}$")) {
            try {
                ctx.channel.sendTyping().queue();
                user = ctx.jda.retrieveUserById(Long.parseUnsignedLong(ctx.rawArgs)).complete();
            } catch (ErrorResponseException ignored) {
                user = null;
            }
        } else if (ctx.rawArgs.matches("^.{2,32}#[0-9]{4}$")) {
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
        } else if (ctx.rawArgs.length() < 1) {
            user = ctx.author;
        } else if (ctx.args.size() > 0 && ctx.args.get(0).equalsIgnoreCase("setup")) {
            cmdProfileSetup(ctx);
            return;
        } else if (ctx.args.size() > 0 && ctx.args.get(0).equalsIgnoreCase("bg")) {
            cmdSetProfileBg(ctx);
            return;
        } else {
            user = null;
        }

        if (user == null) {
            ctx.send(Emotes.getFailure() + " I need a valid @mention, user ID, or user#discriminator!").queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        byte[] data;
        try {
            data = profileCache.get(user);
        } catch (ExecutionException | UncheckedExecutionException e) {
            logger.warn("Error rendering profile", e);
            ctx.send(Emotes.getFailure() + " Failed to render profile!").queue();
            return;
        }

        ctx.channel.sendFile(data, "profile.png", null).queue();
    }

    private static AttributedCharacterIterator fstr(String text, Font font) {
        Font fallbackFont = null;

        int textLength = text.length();

        if (text.startsWith("[B]") && text.endsWith("[/B]")) {
            text = text.substring(3, textLength - 4);
            textLength -= 7;
            font = new Font(font.getName(), Font.BOLD, font.getSize());
        }

        AttributedString result = new AttributedString(text);
        result.addAttribute(TextAttribute.FONT, font, 0, textLength);

        boolean fallback = false;
        int fallbackBegin = 0;
        for (int i = 0; i < text.length(); i++) {
            boolean curFallback = !font.canDisplay(text.charAt(i));

            if (curFallback != fallback) {
                fallback = curFallback;

                if (fallback) {
                    fallbackBegin = i;
                } else {
                    if (fallbackFont == null)
                        fallbackFont = new Font("SansSerif", font.getStyle(), font.getSize());

                    result.addAttribute(TextAttribute.FONT, fallbackFont, fallbackBegin, i);
                }
            }
        }

        return result.getIterator();
    }

    private static void drawMLString(Graphics2D g2d, String text, int x, int y) {
        for (String line: StringUtils.split(text, '\n'))
            g2d.drawString(fstr(line, g2d.getFont()), x, y += g2d.getFontMetrics().getHeight());
    }

    @EventHandler
    public void onUserAvatarUpdate(UserAvatarUpdateEvent event) {
        profileCache.invalidate(event.getUser());
    }

    @Command(name = "profilesetup", desc = "Set up your personal user profile.", thread = true)
    public void cmdProfileSetup(Context ctx) throws SQLException {
        if (profileSetupSessions.contains(ctx.author.getIdLong())) {
            ctx.send(Emotes.getFailure() + " You already have a profile setup session active!").queue();
            return;
        }
        profileSetupSessions.add(ctx.author.getIdLong());

        try {
            ctx.send("Welcome to Profile Setup. I will ask you a series of questions, and you can respond with your answer. If you don't want to answer a certain question, just answer `skip`. If you want to stop this setup, answer `stop`.\n**The questions will now begin.**\n\n\u200b").queue();
            JSONArray answers = new JSONArray();

            for (String question : PROFILE_QUESTIONS) {
                boolean satisfied = false;

                while (!satisfied) {
                    ctx.send(question).queue();
                    Message resp = bot.waitForMessage(300000, m -> m.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                            m.getChannel().getIdLong() == ctx.channel.getIdLong());
                    
                    if (resp == null) {
                        ctx.send(Emotes.getFailure() + " You took too long to respond. Stopping.\nIf you ever want to continue, just invoke this command again.\n**Note**: No answers were saved.").queue();
                        return;
                    }

                    String text = resp.getContent();
                    if (text.equalsIgnoreCase("skip"))
                        text = "¯\\_(ツ)_/¯";
                    else if (text.equalsIgnoreCase("stop")) {
                        ctx.send("Stopping. If you ever want to do it again, just invoke this command again.\n**Note**: No answers were saved.").queue();
                        return;
                    }

                    if (text.length() > (question.equals("Tell me a little bit more about yourself.") ? 250 : 100)) {
                        ctx.send(Emotes.getFailure() + " Answer too long! Try again.").queue();
                        continue;
                    } else if (StringUtils.countMatches(text, '\n') >
                            (question.equals("Tell me a little bit more about yourself.") ? 10 : 2)) {
                        ctx.send(Emotes.getFailure() + " Too many new lines! Try again.").queue();
                        continue;
                    }

                    answers.put(new JSONArray().put(question).put(text));
                    satisfied = true;
                }
            }

            UserProfile profile = profileDao.queryForId(ctx.author.getIdLong());
            if (profile == null)
                profile = new UserProfile(ctx.author.getIdLong(), 0, answers.toString());
            else
                profile.setQuestionValues(answers.toString());

            profileDao.createOrUpdate(profile);
            profileCache.invalidate(ctx.author);

            ctx.send("**Thank you for completing the profile setup!**\nYou may now check your profile using the `profile` command.\n**Tip**: If you want to change your profile background, use `profile bg` or `set_profile_bg`.").queue();
        } finally {
            profileSetupSessions.remove(ctx.author.getIdLong());
        }
    }

    @Command(name = "set_profile_bg", desc = "Set your profile background.",
            usage = "{\"reset\" or \"default\" to reset to default}", thread = true,
            aliases = {"profilebg", "profile_bg", "setprofilebg"})
    public void cmdSetProfileBg(Context ctx) {
        String a = ctx.invoker.equalsIgnoreCase("profile") ?
                ctx.args.get(ctx.args.size() - 1) : ctx.rawArgs;

        if (a.equalsIgnoreCase("reset") || a.equalsIgnoreCase("default")) {
            File path = new File("data/profiles/bg/" + ctx.author.getIdLong() + ".png");

            if (path.exists()) {
                if (path.delete()) {
                    ctx.send(Emotes.getSuccess() + " Background set.").queue();
                } else {
                    ctx.send(Emotes.getFailure() + " Failed to switch background!").queue();
                }
            } else {
                ctx.send(Emotes.getFailure() + " You're **already** using the default background!").queue();
            }
        } else if (ctx.message.getAttachments().size() > 0) {
            ctx.channel.sendTyping().queue();

            try {
                BufferedImage image = ImageIO.read(bot.http.newCall(new Request.Builder()
                        .get()
                        .url(ctx.message.getAttachments().get(0).getUrl())
                        .build()).execute().body().byteStream());

                if (image.getType() != BufferedImage.TYPE_INT_RGB) {
                    BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = newImage.createGraphics();
                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();
                    image = newImage;
                }

                image = GraphicsUtils.resizeImage(image, PROFILE_WIDTH, PROFILE_HEIGHT);

                ImageIO.write(image, "png", new File("data/profiles/bg/" +
                        ctx.author.getIdLong() + ".png"));
                profileCache.invalidate(ctx.author);

                ctx.send(Emotes.getSuccess() + " Background set.").queue();
            } catch (IOException | NullPointerException | IllegalArgumentException ignored) {
                ctx.send(Emotes.getFailure() + " Invalid image! Only GIF, PNG, and JPEG images are supported.").queue();
            }
        } else {
            ctx.send(Emotes.getFailure() + " If you want to use the default background, specify `reset` or `default`. If you want to use a custom background, upload it as an attachment along with your command message. Only GIF, PNG, and JPEG image formats are supported.").queue();
        }
    }
}
