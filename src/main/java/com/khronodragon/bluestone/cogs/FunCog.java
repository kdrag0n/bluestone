package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.emotes.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.util.GraphicsUtils;
import com.khronodragon.bluestone.util.Strings;
import com.khronodragon.bluestone.util.UnisafeString;
import gnu.trove.map.TCharObjectMap;
import gnu.trove.map.TShortObjectMap;
import gnu.trove.map.hash.TCharObjectHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.str;
import static com.khronodragon.bluestone.util.Strings.format;

public class FunCog extends Cog {
    private static final Logger logger = LogManager.getLogger(FunCog.class);
    private static final Map<String, UnisafeString> charsets = new HashMap<String, UnisafeString>() {{
        put("normal", uniString("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("fullwidth", uniString("ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ０１２３４５６７８９～ ｀！＠＃＄％＾＆＊（）－＿＝＋［］｛｝|；：＇＂,＜．＞/？"));
        put("circled", uniString("ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ0①②③④⑤⑥⑦⑧⑨~ `!@#$%^&⊛()⊖_⊜⊕[]{}⦶;:'\",⧀⨀⧁⊘?⦸"));
        put("circled_inverse", uniString("🅐🅑🅒🅓🅔🅕🅖🅗🅘🅙🅚🅛🅜🅝🅞🅟🅠🅡🅢🅣🅤🅥🅦🅧🅨🅩🅐🅑🅒🅓🅔🅕🅖🅗🅘🅙🅚🅛🅜🅝🅞🅟🅠🅡🅢🅣🅤🅥🅦🅧🅨🅩⓿123456789~ `!@#$%^&⊛()⊖_⊜⊕[]{}⦶;:'\",⧀⨀⧁⊘?⦸"));
        put("bold", uniString("𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("bold_old", uniString("𝖆𝖇𝖈𝖉𝖊𝖋𝖌𝖍𝖎𝖏𝖐𝖑𝖒𝖓𝖔𝖕𝖖𝖗𝖘𝖙𝖚𝖛𝖜𝖝𝖞𝖟𝕬𝕭𝕮𝕯𝕰𝕱𝕲𝕳𝕴𝕵𝕶𝕷𝕸𝕹𝕺𝕻𝕼𝕽𝕾𝕿𝖀𝖁𝖂𝖃𝖄𝖅0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("bold_italic", uniString("𝒂𝒃𝒄𝒅𝒆𝒇𝒈𝒉𝒊𝒋𝒌𝒍𝒎𝒏𝒐𝒑𝒒𝒓𝒔𝒕𝒖𝒗𝒘𝒙𝒚𝒛𝑨𝑩𝑪𝑫𝑬𝑭𝑮𝑯𝑰𝑱𝑲𝑳𝑴𝑵𝑶𝑷𝑸𝑹𝑺𝑻𝑼𝑽𝑾𝑿𝒀𝒁0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("bold_script", uniString("𝓪𝓫𝓬𝓭𝓮𝓯𝓰𝓱𝓲𝓳𝓴𝓵𝓶𝓷𝓸𝓹𝓺𝓻𝓼𝓽𝓾𝓿𝔀𝔁𝔂𝔃𝓐𝓑𝓒𝓓𝓔𝓕𝓖𝓗𝓘𝓙𝓚𝓛𝓜𝓝𝓞𝓟𝓠𝓡𝓢𝓣𝓤𝓥𝓦𝓧𝓨𝓩0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("double", uniString("𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("mono", uniString("𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉𝟶𝟷𝟸𝟹𝟺𝟻𝟼𝟽𝟾𝟿~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("sans", uniString("𝖺𝖻𝖼𝖽𝖾𝖿𝗀𝗁𝗂𝗃𝗄𝗅𝗆𝗇𝗈𝗉𝗊𝗋𝗌𝗍𝗎𝗏𝗐𝗑𝗒𝗓𝖠𝖡𝖢𝖣𝖤𝖥𝖦𝖧𝖨𝖩𝖪𝖫𝖬𝖭𝖮𝖯𝖰𝖱𝖲𝖳𝖴𝖵𝖶𝖷𝖸𝖹𝟢𝟣𝟤𝟥𝟦𝟧𝟨𝟩𝟪𝟫~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("sans_bold", uniString("𝗮𝗯𝗰𝗱𝗲𝗳𝗴𝗵𝗶𝗷𝗸𝗹𝗺𝗻𝗼𝗽𝗾𝗿𝘀𝘁𝘂𝘃𝘄𝘅𝘆𝘇𝗔𝗕𝗖𝗗𝗘𝗙𝗚𝗛𝗜𝗝𝗞𝗟𝗠𝗡𝗢𝗣𝗤𝗥𝗦𝗧𝗨𝗩𝗪𝗫𝗬𝗭𝟬𝟭𝟮𝟯𝟰𝟱𝟲𝟳𝟴𝟵~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("sans_bold_italic", uniString("𝙖𝙗𝙘𝙙𝙚𝙛𝙜𝙝𝙞𝙟𝙠𝙡𝙢𝙣𝙤𝙥𝙦𝙧𝙨𝙩𝙪𝙫𝙬𝙭𝙮𝙯𝘼𝘽𝘾𝘿𝙀𝙁𝙂𝙃𝙄𝙅𝙆𝙇𝙈𝙉𝙊𝙋𝙌𝙍𝙎𝙏𝙐𝙑𝙒𝙓𝙔𝙕0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("sans_italic", uniString("𝘢𝘣𝘤𝘥𝘦𝘧𝘨𝘩𝘪𝘫𝘬𝘭𝘮𝘯𝘰𝘱𝘲𝘳𝘴𝘵𝘶𝘷𝘸𝘹𝘺𝘻𝘈𝘉𝘊𝘋𝘌𝘍𝘎𝘏𝘐𝘑𝘒𝘓𝘔𝘕𝘖𝘗𝘘𝘙𝘚𝘛𝘜𝘝𝘞𝘟𝘠𝘡0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("parenthesized", uniString("⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵0⑴⑵⑶⑷⑸⑹⑺⑻⑼~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("regional", uniString("🇦🇧🇨🇩🇪🇫🇬🇭🇮🇯🇰🇱🇲🇳🇴🇵🇶🇷🇸🇹🇺🇻🇼🇽🇾🇿🇦🇧🇨🇩🇪🇫🇬🇭🇮🇯🇰🇱🇲🇳🇴🇵🇶🇷🇸🇹🇺🇻🇼🇽🇾🇿0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
        put("squared", uniString("🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉0123456789~ `!@#$%^&⧆()⊟_=⊞[]{}|;:'\",<⊡>⧄?⧅"));
        put("upside_down", uniString("ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀qƆpƎℲפHIſʞ˥WNOԀQɹS┴∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86~ ,¡@#$%^⅋*)(-‾=+][}{|;:,,,'>˙</¿"));
    }};
    private static final int[] normalChars = {97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 126, 32, 96, 33, 64, 35, 36, 37, 94, 38, 42, 40, 41, 45, 95, 61, 43, 91, 93, 123, 125, 124, 59, 58, 39, 34, 44, 60, 46, 62, 47, 63};
    private static final TShortObjectMap<WebhookMessage> numToBleachMsg = new TShortObjectHashMap<>();
    private static final TCharObjectMap<String> alphabetToEmote = new TCharObjectHashMap<>() {{
        put(' ', "    ");
        put('#', "#⃣");
        put('!', "❗");
        put('?', "❓");
        put('$', "💲");
        put('-', "➖");
        put('.', "🔹");
        put('~', "〰");
        put('0', ":zero:");
        put('1', ":one:");
        put('2', ":two:");
        put('3', ":three:");
        put('4', ":four:");
        put('5', ":five:");
        put('6', ":six:");
        put('7', ":seven:");
        put('8', ":eight:");
        put('9', ":nine:");
        put('^', "⬆");
        put('a', ":regional_indicator_a:");
        put('b', ":regional_indicator_b:");
        put('c', ":regional_indicator_c:");
        put('d', ":regional_indicator_d:");
        put('e', ":regional_indicator_e:");
        put('f', ":regional_indicator_f:");
        put('g', ":regional_indicator_g:");
        put('h', ":regional_indicator_h:");
        put('i', ":regional_indicator_i:");
        put('j', ":regional_indicator_j:");
        put('k', ":regional_indicator_k:");
        put('l', ":regional_indicator_l:");
        put('m', ":regional_indicator_m:");
        put('n', ":regional_indicator_n:");
        put('o', ":regional_indicator_o:");
        put('p', ":regional_indicator_p:");
        put('q', ":regional_indicator_q:");
        put('r', ":regional_indicator_r:");
        put('s', ":regional_indicator_s:");
        put('t', ":regional_indicator_t:");
        put('u', ":regional_indicator_u:");
        put('v', ":regional_indicator_v:");
        put('w', ":regional_indicator_w:");
        put('x', ":regional_indicator_x:");
        put('y', ":regional_indicator_y:");
        put('z', ":regional_indicator_z:");
    }};
    private static final String[] ADJECTIVES = {"lovingly",
            "lamely",
            "limply",
            "officially",
            "for money",
            "sadly",
            "roughly",
            "angrily",
            "harshly",
            "without hesitation",
            "quickly",
            "greedily",
            "shamefully",
            "dreadfully",
            "painfully",
            "intensely",
            "digitally",
            "unofficially",
            "nervously",
            "invitingly",
            "seductively",
            "embarassingly",
            "thoroughly",
            "doubtfully",
            "proudly"};
    private static final String[] FIGHTS = {"pokes {0} with a spear",
            "impales {0}",
            "stabs {0}",
            "guts {0} with a stone knife",
            "eviscerates {0} with a sharp stone",
            "decapitates {0} with a wand",
            "fires cruise missle at {0}",
            "backstabs {0}",
            "punches {0}",
            "poisons {0}",
            "opens trapdoor under {0}",
            "360 quick scopes {0}",
            "noscopes {0}",
            "normally snipes {0}",
            "uses katana to slice through {0}",
            "deadily stares at {0}",
            "uses a trebuchet to shoot a 95kg projectile over 300 meters at {0}",
            "snaps neck from {0}",
            "pours lava over {0}",
            "dumps acid above {0}",
            "shoots with a glock 17 at {0}",
            "incinerates {0}",
            "uses a tridagger to stab {0}",
            "assasinates {0}",
            "fires with a minigun at {0}",
            "fires with bazooka at {0}",
            "uses granny bomb at {0}",
            "throws bananabomb at {0}",
            "throws holy grenade at {0}"};
    private static final String[] DEATHS = {"{0} dies.",
            "{0} survives.",
            "Blood pours from {0}.",
            "{0} heals themself.",
            "Fairies take {0} away.",
            "An old man carries {0} away.",
            "{0} is in shock.",
            "{0} passes out."};
    private static final Color BLEACH_COLOR = new Color(51, 143, 216);
    private static final MessageEmbed BLEACH_EMBED = new EmbedBuilder()
            .setColor(BLEACH_COLOR)
            .setTitle("Bleach")
            .setImage("https://upload.wikimedia.org/wikipedia/commons/d/d3/Clorox_Bleach_products.jpg")
            .build();
    private final EmoteProviderManager emoteProviderManager = new EmoteProviderManager();

    static {
        List<MessageEmbed> list = new LinkedList<>();

        for (short i = 1; i <= 10; i++) {
            list.add(BLEACH_EMBED);

            numToBleachMsg.put(i, new WebhookMessageBuilder()
                    .setUsername("Bleach Delivery")
                    .setAvatarUrl("https://upload.wikimedia.org/wikipedia/commons/d/d3/Clorox_Bleach_products.jpg")
                    .addEmbeds(list)
                    .build());
        }
    }

    private static final UnisafeString uniString(String javaString) {
        return new UnisafeString(javaString);
    }

    public FunCog(Bot bot) {
        super(bot);

        OkHttpClient http = new OkHttpClient();
        emoteProviderManager.addProvider(new TwitchEmoteProvider(http));
        emoteProviderManager.addProvider(new BetterTTVEmoteProvider(http));
        emoteProviderManager.addProvider(new FrankerFaceZEmoteProvider(http));
        emoteProviderManager.addProvider(new DiscordEmoteProvider());
    }

    public String getName() {
        return "Fun";
    }

    public String getDescription() {
        return "Who doesn't like fun?";
    }

    @Command(name = "reverse", desc = "Reverse some text.", usage = "[text]")
    public void cmdReverse(Context ctx) {
        ctx.send(":repeat: " + StringUtils.reverse(ctx.rawArgs)).queue();
    }

    @Command(name = "emotisay", desc = "Show some text as cool block letters.", aliases = {"emotesay", "esay"}, usage = "[text]")
    public void cmdEmotisay(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " You need some text!").queue();
            return;
        }

        ctx.send(Strings.simpleJoin(ctx.rawArgs.chars().mapToObj(c -> {
            final char character = Character.toLowerCase((char) c);

            if (alphabetToEmote.containsKey(character)) {
                return alphabetToEmote.get(character);
            } else {
                return String.valueOf(character);
            }
        }).collect(Collectors.toList()))).queue();
    }

    @Command(name = "cookie", desc = "Cookie time!")
    public void cmdCookie(Context ctx) {
        ctx.send("\uD83C\uDF6A").queue();
    }

    @Command(name = "triggered", desc = "TRIGGERED")
    public void cmdTriggered(Context ctx) {
        ctx.send("***TRIGGERED***").queue();
    }

    @Command(name = "lenny", desc = "Le Lenny Face.")
    public void cmdLenny(Context ctx) {
        ctx.send("( ͡° ͜ʖ ͡°)").queue();
    }

    @Command(name = "tableflip", desc = "Flip that table!")
    public void cmdTableflip(Context ctx) {
        ctx.send("(╯°□°）╯︵ ┻━┻").queue();
    }

    @Command(name = "unflip", desc = "Flip that table back up!")
    public void cmdUnflip(Context ctx) {
        ctx.send("┬─┬\uFEFF ノ( ゜-゜ノ)").queue();
    }

    @Command(name = "hyflip", desc = "Is that table flipped or not? Oh wait, it's broken...")
    public void cmdHyflip(Context ctx) {
        ctx.send("(╯°□°）╯︵ ┻━─┬\uFEFF ノ( ゜-゜ノ)").queue();
    }

    @Command(name = "bleach", desc = "Get me some bleach. NOW.")
    public void cmdBleach(Context ctx) {
        ctx.send(BLEACH_EMBED).queue();
    }

    @Command(name = "cat", desc = "Get a random cat!", thread = true, aliases = {"randcat"})
    public void cmdCat(Context ctx) {
        ctx.channel.sendTyping().queue();

        try {
            String cat = new JSONObject(Bot.http.newCall(new Request.Builder()
                    .get()
                    .url("https://random.cat/meow")
                    .build()).execute().body().string()).optString("file", null);
            String fact = new JSONObject(Bot.http.newCall(new Request.Builder()
                    .get()
                    .url("https://catfact.ninja/fact")
                    .build()).execute().body().string()).optString("fact", null);

            if (cat == null || fact == null) {
                ctx.send(Emotes.getFailure() + " Couldn't get a cat!").queue();
                return;
            }

            Color color;
            if (ctx.guild == null)
                color = randomColor();
            else
                color = val(ctx.member.getColor()).or(Color.WHITE);

            ctx.send(new EmbedBuilder()
                    .setImage(cat)
                    .setColor(color)
                    .setAuthor("Random Cat", null, "https://khronodragon.com/cat.png")
                    .addField("Did You Know?", fact, false)
                    .build()).queue();
        } catch (IOException ignored) {
            ctx.send(Emotes.getFailure() + " Failed to get a cat!").queue();
        }
    }

    @Command(name = "dog", desc = "Get a random dog!", thread = true, aliases = {"randdog"})
    public void cmdDog(Context ctx) {
        ctx.channel.sendTyping().queue();

        try {
            String cat = new JSONObject(Bot.http.newCall(new Request.Builder()
                    .get()
                    .url("https://dog.ceo/api/breeds/image/random")
                    .build()).execute().body().string()).optString("message", null);
            String fact = val(new JSONObject(Bot.http.newCall(new Request.Builder()
                    .get()
                    .url("https://dog-api.kinduff.com/api/facts?number=1")
                    .build()).execute().body().string()).optJSONArray("facts")).or(new JSONArray())
                            .optString(0, null);

            if (cat == null || fact == null) {
                ctx.send(Emotes.getFailure() + " Couldn't get a dog!").queue();
                return;
            }

            Color color;
            if (ctx.guild == null)
                color = randomColor();
            else
                color = val(ctx.member.getColor()).or(Color.WHITE);

            ctx.send(new EmbedBuilder()
                    .setImage(cat)
                    .setColor(color)
                    .setAuthor("Random Dog", null, "https://khronodragon.com/dog.png")
                    .addField("Did You Know?", fact, false)
                    .build()).queue();
        } catch (IOException ignored) {
            ctx.send(Emotes.getFailure() + " Failed to get a dog!").queue();
        }
    }

    @Command(name = "emote", desc = "Get an emoticon, from many sources.", usage = "[emote name]")
    public void cmdEmote(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " You need to specify an emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.").queue();
            return;
        }

        String eName = ctx.args.get(0);
        if (eName.equalsIgnoreCase("add")) {
            cmdAddEmote(ctx);
            return;
        }

        final String url = emoteProviderManager.getFirstUrl(eName);
        if (url == null) {
            ctx.send(Emotes.getFailure() + " No such emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.").queue();
            return;
        }
        EmoteInfo info = emoteProviderManager.getFirstInfo(eName);

        Bot.http.newCall(new Request.Builder()
                .get()
                .url(url)
                .build()).enqueue(Bot.callback(response -> {
            Message msg = null;

            if (info.description != null && info.description.length() > 0) {
                msg = new MessageBuilder()
                        .append(info.description)
                        .build();
            }

            ctx.channel.sendFile(response.body().byteStream(), "emote.png", msg).queue();
        }, e -> ctx.send(Emotes.getFailure() + " Failed to fetch emote.").queue()));
    }

    @Perm.ManageEmotes
    @Command(name = "add_emote", desc = "Add an emote to the server.", usage = "[emote name]",
            aliases = {"addemote", "emoteadd", "emote_add", "+emote", "+e"}, guildOnly = true)
    public void cmdAddEmote(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " You need to specify an emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_EMOTES)) {
            ctx.send(Emotes.getFailure() + " I need to be able to **manage emotes**!").queue();
            return;
        }

        String eName = ctx.args.get(0);
        if (eName.equalsIgnoreCase("add") && ctx.args.size() > 1)
            eName = ctx.args.get(1);
        final String n = eName;

        final String url = emoteProviderManager.getFirstUrl(eName);
        if (url == null) {
            ctx.send(Emotes.getFailure() + " No such emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.").queue();
            return;
        }

        ctx.channel.sendTyping().queue();

        Bot.http.newCall(new Request.Builder()
                .get()
                .url(url)
                .build()).enqueue(Bot.callback(response -> {
            final String nm = url.startsWith("https://cdn.discordapp.com/emojis/")
                    ? emoteProviderManager.getFirstInfo(n).name : n;

            InputStream is = response.body().byteStream();
            Emote emote = ctx.guild.getController().createEmote(nm, Icon.from(is))
                    .reason("Adding emote " + nm + " by user request. User has Manage Emotes.")
                    .complete();

            ctx.send(Emotes.getSuccess() + " Emote added. " + emote.getAsMention()).queue();
        }, e -> ctx.send(Emotes.getFailure() + " Failed to fetch or create emote.").queue()));
    }

    @Perm.ManageEmotes
    @Command(name = "add_compound_emote", desc = "Add a compound emote to the server.", guildOnly = true,
            aliases = {"add_cmp_emote", "addcemote", "addcmpemote", "cmp_add", "cadd",
                    "cemote_add", "cmp_emote_add", "+ce", "+cmp", "+compound", "+cemote"},
            usage = "[emote name] [attach an image]")
    public void cmdAddCompoundEmote(Context ctx) {
        String baseName;
        Message.Attachment attachment;
        if (ctx.rawArgs.length() < 1 || !Strings.isEmoteName(baseName = ctx.args.get(0))) {
            ctx.send(Emotes.getFailure() + " Invalid emote name! Must be between 2 and 32 characters in length.").queue();
            return;
        } else if (ctx.message.getAttachments().size() < 1 || !(attachment = ctx.message.getAttachments().get(0)).isImage()) {
            ctx.send(Emotes.getFailure() + " You must upload an image!").queue();
            return;
        }

        ctx.channel.sendTyping().queue();

        Bot.http.newCall(new Request.Builder()
                .get()
                .url(attachment.getProxyUrl())
                .build()).enqueue(Bot.callback(resp -> {
            InputStream is = resp.body().byteStream();
            BufferedImage base;

            // Load image
            try {
                base = ImageIO.read(is);
            } catch (IOException | NullPointerException | IllegalArgumentException ignored) {
                ctx.send(Emotes.getFailure() + " Invalid image! Only GIF, PNG, and JPEG images are supported.").queue();
                return;
            } catch (ArrayIndexOutOfBoundsException ignored) {
                ctx.send(Emotes.getFailure() + " Your image seems to be in a weird format, or corrupted...").queue();
                return;
            } finally {
                IOUtils.closeQuietly(is);
            }

            // Crop to square based on smallest dimension, if not already exactly square
            int dim;
            if (base.getWidth() != base.getHeight()) {
                dim = Math.min(base.getWidth(), base.getHeight());
                base = base.getSubimage(0, 0, dim, dim);
            } else {
                dim = base.getWidth();
            }

            // Init some variables
            final int emoteImageDim = 112;
            final int maxEmoteDim = 5;

            // Dimension verification
            int emoteDim = Math.round((float) dim / (float) emoteImageDim);
            if (emoteDim > maxEmoteDim) {
                emoteDim = maxEmoteDim;
            }

            int targetDim = emoteImageDim * emoteDim;
            base = GraphicsUtils.resizeImage(base, targetDim, targetDim);
            dim = targetDim;

            // Init some variables
            List<byte[]> results = new ArrayList<>(emoteDim * emoteDim);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            // Do actual emote cropping
            for (int x = 0; x < emoteDim; x++) {
                for (int y = 0; y < emoteDim; y++) {
                    BufferedImage section = base.getSubimage(emoteImageDim * x, emoteImageDim * y,
                            emoteImageDim, emoteImageDim);

                    ImageIO.write(section, "png", stream);
                    results.add(stream.toByteArray());

                    stream.reset();
                }
            }

            ctx.channel.sendTyping().queue();
            OffsetDateTime lastSentTyping = OffsetDateTime.now();
            List<Emote> finalEmotes = new ArrayList<>(results.size());

            for (int i = 0; i < results.size(); i++) {
                Emote emote =
                        ctx.guild.getController().createEmote(baseName + (i + 1), Icon.from(results.get(i)))
                        .reason("Creating emote " + (i + 1) + " of " + results.size() +
                                " for " + emoteDim + "x" + emoteDim + " compound emote \"" + baseName + '"')
                        .complete();

                finalEmotes.add(emote);

                OffsetDateTime now = OffsetDateTime.now();
                if (lastSentTyping.isBefore(now.minusSeconds(14))) {
                    ctx.channel.sendTyping().queue();
                    lastSentTyping = now;
                }

                Thread.sleep(100);
            }

            // Render and send the result
            StringBuilder usageAll = new StringBuilder((baseName.length() + 4) * finalEmotes.size() + emoteDim);
            StringBuilder renderAll = new StringBuilder((baseName.length() + 25) * finalEmotes.size() + emoteDim);

            int ix = 1;
            for (int i = 0; i < finalEmotes.size(); i++) {
                Emote emote = finalEmotes.get(i);

                // add to usageAll
                usageAll.append(':')
                        .append(emote.getName())
                        .append(':');

                // add to renderAll
                renderAll.append(emote.getAsMention());

                if (ix == 5) {
                    usageAll.append('\n');
                    renderAll.append('\n');
                    ix = 0;
                }

                ix++;
            }

            ctx.send(Emotes.getSuccess() + " Compound emote created.\n\nUsage:\n```" + usageAll.toString() +
                    "```\n\nResult:\n" + renderAll.toString()).queue();
        }, e -> {
            logger.error("Error creating compound emote", e);
            ctx.send(Emotes.getFailure() + " Failed to create compound emote.").queue();
        }));
    }

    private String applyStyle(String orig, UnisafeString mapTo) {
        UnisafeString mapFrom = charsets.get("normal");
        StringBuilder newString = new StringBuilder();

        orig.codePoints().map(i -> {
            if (ArrayUtils.contains(normalChars, i)) {
                return mapTo.charAt(mapFrom.indexOf(i) - 1);
            } else {
                return i;
            }
        }).forEach(newString::appendCodePoint);

        return StringUtils.replace(newString.toString(), "_", "\\_");
    }

    @Command(name = "styles", desc = "List the available text styles.", aliases = {"fonts"})
    public void cmdStyles(Context ctx) {
        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor("Text Styles", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setColor(randomColor())
                .setDescription("\u200b")
                .setTimestamp(Instant.now());

        for (Map.Entry<String, UnisafeString> entry: charsets.entrySet()) {
            emb.appendDescription("    \u2022 " + applyStyle(entry.getKey(), entry.getValue()) + "\n");
        }
        emb.appendDescription("\n\nUse a style with the `style` command: `style [name] [text]`.");

        ctx.send(emb.build()).queue();
    }

    @Command(name = "style", desc = "Apply a style to some text.", aliases = {"font"})
    public void cmdStyle(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " Usage is `style [style name] [text]`.\n" +
                    "\nTip: *use the `styles` command to see what there is.*").queue();
            return;
        }
        if (ctx.args.size() < 2) {
            ctx.send(Emotes.getFailure() + " Usage is `style [style name] [text]`.").queue();
            return;
        }

        String styleName = ctx.args.get(0);
        if (!charsets.containsKey(styleName)) {
            ctx.send(Emotes.getFailure() + " No such style! List them with the `styles` command.").queue();
            return;
        }

        String text = ctx.rawArgs.substring(styleName.length()).trim();
        ctx.send(applyStyle(text, charsets.get(styleName))).queue();
    }

    @Command(name = "lmgtfy", desc = "Let me Google that for you!")
    public void cmdLmgtfy(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need some search terms!").queue();
            return;
        }

        ctx.send("<http://lmgtfy.com/?q=" + ctx.args.stream()
                .map(s -> StringUtils.replace(s, "+", "%2B"))
                .collect(Collectors.joining("+")) + ">").queue();
    }

    @Command(name = "slap", desc = "Slap someone, with passion.", aliases = {"boop", "poke", "hit"})
    public void cmdSlap(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need someone to " + ctx.invoker + "!").queue();
            return;
        }

        ctx.send(format("{0} {1}s *{2}* **{3}**.", (ctx.guild == null ? ctx.author : ctx.member).getAsMention(),
                ctx.invoker, ctx.rawArgs, randomChoice(ADJECTIVES))).queue();
    }

    @Command(name = "attack", desc = "Hurt someone, with determination.", aliases = {"stab", "kill", "punch", "shoot", "hurt", "fight"})
    public void cmdAttack(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need someone to " + ctx.invoker + "!").queue();
            return;
        }
        final String target = '*' + ctx.rawArgs + '*';

        ctx.send((ctx.guild == null ? ctx.author : ctx.member).getAsMention() + ' ' +
                format(randomChoice(FIGHTS), target) + ". " + format(randomChoice(DEATHS), target)).queue();
    }

    @Command(name = "charlie", desc = "Ask a question... Charlie Charlie are you there?")
    public void cmdCharlie(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need a question!").queue();
            return;
        }
        String question = ctx.rawArgs.endsWith("?") ? ctx.rawArgs : ctx.rawArgs + "?";

        ctx.send("*Charlie Charlie* " + question + "\n**" + (randint(0, 1) == 1 ? "Yes" : "No") + "**").queue();
    }

    @Command(name = "soon", desc = "Feel the loading of 10000 years, aka Soon™.", aliases = {"soontm"})
    public void cmdSoon(Context ctx) {
        ctx.channel.sendFile(FunCog.class.getResourceAsStream("/assets/soon.gif"),
                "soon.gif", null).queue();
    }

    @Perm.Patron
    @Command(name = "multibleach", desc = "Get multiple loads of bleach.", usage = "[amount of bleach]",
            aliases = {"mbleach", "mb"}, guildOnly = true)
    public void cmdMultiBleach(Context ctx) {
        short n;
        try {
            if ((n = Short.parseShort(ctx.args.get(0))) < 2 || n > 10) {
                ctx.send(Emotes.getFailure() + " You must specify between 2 and 10 bleach!").queue();
                return;
            }
        } catch (IndexOutOfBoundsException|NumberFormatException ignored) {
            ctx.send(Emotes.getFailure() + " You must specify between 2 and 10 bleach!").queue();
            return;
        }

        if (!ctx.guild.getSelfMember().hasPermission((TextChannel) ctx.channel, Permission.MANAGE_WEBHOOKS)) {
            ctx.send(Emotes.getFailure() + " I need to be able to **manage webhooks** here!").queue();
            return;
        }

        ctx.guild.getWebhooks().queue(wl -> {
            Webhook hook = wl.stream()
                    .filter(w -> w.getName().equals("multibleach for #" + ctx.channel.getName()))
                    .findFirst()
                    .orElse(null);

            if (hook == null) {
                ((TextChannel) ctx.channel).createWebhook("multibleach for #" + ctx.channel.getName())
                        .queue(h -> {
                            mb2(h, n);
                        }, e -> ctx.send(Emotes.getFailure() + " Failed to create webhook."));
                return;
            }

            mb2(hook, n);
        }, e -> ctx.send(Emotes.getFailure() + " Failed to fetch webhooks.").queue());
    }

    private void mb2(Webhook hook, short n) {
        WebhookClient client = new WebhookClientBuilder(hook)
                .setHttpClient(Bot.http)
                .setDaemon(true)
                .setExecutorService(bot.getScheduledExecutor())
                .build();
        client.send(numToBleachMsg.get(n));
        client.close();
    }

    @Command(name = "rps", desc = "Play a game of Rock, Paper, Scissors with the bot. (10 rounds)",
            aliases = {"rockpaperscissors", "rock,paper,scissors"})
    public void cmdRps(Context ctx) {
        if (ctx.channel instanceof TextChannel &&
                !ctx.member.hasPermission((Channel) ctx.channel,
                        Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS)) {
            ctx.send(Emotes.getFailure() +
                    " I need to be able to **add reactions** and **embed links** here!").queue();
            return;
        }

        new RpsGame(ctx);
    }

    private final class RpsGame {
        private final Object[] REACTIONS = {"🗿", "📰", "✂", "⛔"};
        private final String[] IDX_CMAP = {"Rock", "Paper", "Scissors"};
        private final String[] IDX_MAP = {"rock", "paper", "scissors"};
        private final String[] WIN_STATUSES = {"seems to win over", "wins over", "beats", "goes over", "destroys",
                "eliminates", "takes down", "dramatically incinerates", "overpowers", "wrecks", "blows up", "escapes",
                "dominates", "is clearly superior to", "is superior to", "is definitely better than", "tackles"};
        private final String[] LOSE_STATUSES = {"can't beat", "loses to", "is taken down by", "goes under",
                "is tortured by", "cowers under", "hides from", "runs away from", "flees from", "begs for mercy from",
                "can't take on", "fails to hold its own against", "obeys", "bows down to"};
        private final String[] WIN_PREFIX = {"You're in luck.", "You win!", "I bow down to you.",
                "I'm crying right now.", "You dominate me.", "It's your lucky day.", "You're doing well.", "Good job.",
                "You fiercely take me down.", "You just tackle me. That can't be.", "Me, a puny little bot, is nothing for you.",
                "RIP me.", "I can't beat you. 😭", "I admit, I'm dumb. Happy now?", "You played well.", "Good choice.", "Smart choice.",
                "I'm is no match for you. But you're just a puny little human!", "CHEATER! That CAN'T be! I always win!"};
        private final String[] WIN_SUFFIX = {"That makes sense for you.", "Just what I expected.", "I expected nothing less.",
                "Good job.", "You did better than I thought. Maybe puny humans like you have some worth.", "I'm surprised.",
                "You weren't supposed to do so good.", "That's just wrong.", "Nice playing.", "Nice luck.", "Nice shot.",
                "You're doing a bit *too* good. Slow down, buddy.", "Whoa, whoa. Slow down there, buddy."};
        private final String[] LOSE_PREFIX = {"Uh-oh.", "RIP.", "The mighty AI takes you down.", "You're no match for the AI.",
                "You're no more.", "You helplessly cry as the AI's weapon of choice tears you apart.", "0/10 WANT MORE ACTION. 2ez.",
                "Too easy!", "You should be better than this.", "How can you not win? It's so easy.", "Amusing.",
                "WANT. MORE. ACTION. NOW.", "Has anyone ever told you that you suck? If not, well, you do.", "Noh.",
                "Not this time, human. And never.", "Boo-hoo. You're dead."};
        private final String[] LOSE_SUFFIX = {"It was worth a shot.", "At least you played.", "Better luck next time.",
                "0/10 Not enough action, IGN.", "You should be better than this.", "I expected more.", "Can't you do such a simple thing?",
                "I guess humans have no hope.", "Hopeless little humans... You're a better dinner than this.", "Dinner, mmm.",
                "Are you capable of winning? I doubt it.", "Meh, I'm not even sweating. You?", "Oh nooo. I was so scared."};

        private final EmbedBuilder emb = new EmbedBuilder();
        private Message message;
        private final Runnable onFinish;
        private final MessageChannel channel;
        private final long userId;

        private short plays = 0;
        private short wins = 0;
        private short losses = 0;
        private boolean isActive = true;

        private RpsGame(Context ctx) {
            this.channel = ctx.channel;
            this.userId = ctx.author.getIdLong();

            emb.setAuthor("Rock, Paper, Scissors!", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                    .setDescription("⌛ **Please wait, game is starting...**")
                    .setFooter("Game started at", ctx.author.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            if (ctx.guild == null)
                emb.setColor(randomColor());
            else
                emb.setColor(val(ctx.member.getColor()).or(Color.WHITE));

            message = channel.sendMessage(emb.build()).complete();
            for (Object emoji: REACTIONS) {
                message.addReaction((String) emoji).complete();
            }
            next();

            onFinish = () -> {
                String descSuffix = (wins == losses ? "That means you tied. " + loseSuffix() :
                        (wins > losses ? "That makes you the winner. " + winSuffix() :
                                "That makes you the loser. " + loseSuffix()));
                emb.setDescription("❌ Game ended.\n\nYou won **" + wins + "** times, and lost **" + losses + "** times." +
                        '\n' + descSuffix);

                message.editMessage(emb.build()).queue();
                try {
                    message.clearReactions().queue();
                } catch (ErrorResponseException | PermissionException ignored) {}
                catch (IllegalStateException ignored) { // DM
                    for (MessageReaction reaction: message.getReactions()) {
                        reaction.removeReaction().queue();
                        reaction.removeReaction(ctx.author).queue();
                    }
                }

                isActive = false;
            };

            scheduleEventWait(ctx);
        }

        private void scheduleEventWait(Context ctx) {
            bot.getEventWaiter().waitForEvent(MessageReactionAddEvent.class, ev -> {
                return ev.getChannel().getIdLong() == channel.getIdLong() &&
                        ev.getMessageIdLong() == message.getIdLong() && ev.getUser().getIdLong() == userId;
            }, ev -> {
                if (!isActive) return;

                byte answer = (byte) ArrayUtils.indexOf(REACTIONS, ev.getReactionEmote().getName());

                if (answer == 3) {
                    emb.clearFields()
                            .addField("Status", "Game was stopped before the end!", false);
                    onFinish.run();
                    return;
                }

                try {
                    ans(answer);
                } finally {
                    try {
                        ev.getReaction().removeReaction(ctx.author).queue();
                    } catch (Throwable ignored) {}

                    scheduleEventWait(ctx);
                }
            }, 2, TimeUnit.MINUTES, onFinish);
        }

        private void ans(byte answer) {
            byte ai = (byte) ThreadLocalRandom.current().nextInt(0, 2);
            if (answer == ai) {
                emb.clearFields()
                        .addField("Round #" + plays,
                                "**Tie!** Try again.\n\nRock, paper, or scissors?", false);
                message.editMessage(emb.build()).queue();
                return;
            }
            boolean win;
            
            if (answer==0 && ai==2) win = true;
            else if (answer==0 && ai==1) win = false;
            else if (answer==1 && ai==2) win = false;
            else if (answer==1 && ai==0) win = true;
            else if (answer==2 && ai==1) win = true;
            else if (answer==2 && ai==0) win = false;
            else win = false;

            boolean flip = ThreadLocalRandom.current().nextBoolean();
            int a;
            int b;
            if (flip) {
                a = (int) ai;
                b = (int) answer;
            } else {
                a = (int) answer;
                b = (int) ai;
            }

            String msg = IDX_CMAP[a] + ' ' + (flip ? (win ? loseStatus() : winStatus()) :
                    (win ? winStatus() : loseStatus())) + ' ' + IDX_MAP[b] + '.';

            boolean wlSide = ThreadLocalRandom.current().nextBoolean();
            if (wlSide) {
                if (win) {
                    msg = winPrefix() + ' ' + msg;
                } else {
                    msg = losePrefix() + ' ' + msg;
                }
            } else {
                if (win) {
                    msg = msg + ' ' + winSuffix();
                } else {
                    msg = msg + ' ' + loseSuffix();
                }
            }

            emb.clearFields()
                    .addField("Round #" + plays, msg, false);
            message.editMessage(emb.build()).queue();

            if (win) ++wins;
            else ++losses;
            next();
        }

        private String winStatus() {
            return WIN_STATUSES[ThreadLocalRandom.current().nextInt(0, WIN_STATUSES.length)];
        }

        private String loseStatus() {
            return LOSE_STATUSES[ThreadLocalRandom.current().nextInt(0, LOSE_STATUSES.length)];
        }

        private String winPrefix() {
            return WIN_PREFIX[ThreadLocalRandom.current().nextInt(0, WIN_PREFIX.length)];
        }

        private String losePrefix() {
            return LOSE_PREFIX[ThreadLocalRandom.current().nextInt(0, LOSE_PREFIX.length)];
        }

        private String winSuffix() {
            return WIN_SUFFIX[ThreadLocalRandom.current().nextInt(0, WIN_SUFFIX.length)];
        }

        private String loseSuffix() {
            return LOSE_SUFFIX[ThreadLocalRandom.current().nextInt(0, LOSE_SUFFIX.length)];
        }

        private void next() {
            if (plays == 10) {
                onFinish.run();
                return;
            }

            emb.setDescription(null)
                    .addField("Round #" + ++plays, "Rock, paper, or scissors?", false);

            message.editMessage(emb.build()).queue();
        }
    }

    @Command(name = "akinator", desc = "Play a game of Akinator, where you answer questions for it to guess your character.",
            aliases = {"guess"})
    public void cmdAkinator(Context ctx) {
        if (ctx.channel instanceof TextChannel &&
                !ctx.member.hasPermission((Channel) ctx.channel,
                        Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS)) {
            ctx.send(Emotes.getFailure() +
                    " I need to be able to **add reactions** and **embed links** here!").queue();
            return;
        }

        try {
            new AkinatorGame(ctx);
        } catch (IOException e) {
            logger.error("Error contacting Akinator", e);
            ctx.send(Emotes.getFailure() + " An error occurred contacting Akinator.").queue();
        } catch (JSONException ignored) {
            ctx.send(Emotes.getFailure() + " Akinator seems to be having some issues right now.").queue();
        }
    }

    private final class AkinatorGame {
        private static final String NEW_SESSION_URL = "http://api-en4.akinator.com/ws/new_session?partner=1&player=";
        private static final String ANSWER_URL = "http://api-en4.akinator.com/ws/answer";
        private static final String GET_GUESS_URL = "http://api-en4.akinator.com/ws/list";
        private static final String CHOICE_URL = "http://api-en4.akinator.com/ws/choice";
        private static final String EXCLUSION_URL = "http://api-en4.akinator.com/ws/exclusion";
        private final Object[] REACTIONS = {"✅", "❌", "🤷", "👍", "👎", "⛔"};

        private final OkHttpClient client = new OkHttpClient();
        private final EmbedBuilder emb = new EmbedBuilder();
        private Message message;
        private final Runnable onFinish;
        private final MessageChannel channel;
        private final long userId;
        private StepInfo stepInfo;

        private final String signature;
        private final String session;
        private Guess guess;
        private boolean lastQuestionWasGuess = false;
        private boolean isActive = true;

        private AkinatorGame(Context ctx) throws IOException, JSONException {
            this.channel = ctx.channel;
            this.userId = ctx.author.getIdLong();

            // Start new session
            JSONObject json = new JSONObject(client.newCall(new Request.Builder()
                    .get()
                    .url(NEW_SESSION_URL + RandomStringUtils.random(16))
                    .build()).execute().body().string());
            stepInfo = new StepInfo(json);

            signature = stepInfo.getSignature();
            session = stepInfo.getSession();

            emb.setAuthor("Akinator Game", "http://akinator.com", ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                    .setDescription("⌛ **Please wait, game is starting...**")
                    .setFooter("Game started at", ctx.author.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            if (ctx.guild == null)
                emb.setColor(randomColor());
            else
                emb.setColor(val(ctx.member.getColor()).or(Color.WHITE));

            message = channel.sendMessage(emb.build()).complete();
            for (Object emoji: REACTIONS) {
                message.addReaction((String) emoji).complete();
            }
            presentNextQuestion();

            onFinish = () -> {
                emb.setDescription("❌ Game ended.\n\nThere w");
                if (stepInfo.getStepNum() == 0)
                    emb.appendDescription("as 1 question");
                else
                    emb.getDescriptionBuilder()
                            .append("ere ")
                            .append(stepInfo.getStepNum() + 1)
                            .append(" questions");
                emb.getDescriptionBuilder().append('.');

                message.editMessage(emb.build()).queue();
                try {
                    message.clearReactions().queue();
                } catch (ErrorResponseException | PermissionException ignored) {}
                catch (IllegalStateException ignored) { // DM
                    for (MessageReaction reaction: message.getReactions()) {
                        reaction.removeReaction().queue();
                        reaction.removeReaction(ctx.author).queue();
                    }
                }

                isActive = false;
            };

            scheduleEventWait(ctx);
        }

        private void scheduleEventWait(Context ctx) {
            bot.getEventWaiter().waitForEvent(MessageReactionAddEvent.class, ev -> {
                return ev.getChannel().getIdLong() == channel.getIdLong() &&
                        ev.getMessageIdLong() == message.getIdLong() && ev.getUser().getIdLong() == userId;
            }, ev -> {
                if (!isActive) return;

                byte answer = (byte) ArrayUtils.indexOf(REACTIONS, ev.getReactionEmote().getName());

                if (answer == 5) {
                    emb.setImage(null)
                            .clearFields()
                            .addField("Status", "Game was stopped before the end!", false);
                    onFinish.run();
                    return;
                }

                try {
                    if (lastQuestionWasGuess) {
                        if (answer != 0 && answer != 1)
                            return;

                        answerGuess(answer);
                    } else {
                        answerQuestion(answer);
                    }
                } finally {
                    try {
                        ev.getReaction().removeReaction(ctx.author).queue();
                    } catch (Throwable ignored) {}

                    scheduleEventWait(ctx);
                }
            }, 2, TimeUnit.MINUTES, onFinish);
        }

        private void endInvalidData() {
            emb.setImage(null)
                    .clearFields()
                    .addField("Status", "Akinator sent invalid data, or failed to respond.", false);
            onFinish.run();
        }

        private void presentNextQuestion() {
            emb.setDescription(null)
                    .clearFields()
                    .setImage(null)
                    .addField("Question #" + (stepInfo.getStepNum() + 1), stepInfo.getQuestion(), false);

            message.editMessage(emb.build()).queue();
            lastQuestionWasGuess = false;
        }

        private void presentGuess() throws IOException {
            try {
                guess = new Guess();
            } catch (JSONException|IOException ignored) {
                endInvalidData();
                return;
            }

            emb.clearFields()
                    .addField("Is this your character?", guess.toString(), false)
                    .setImage(guess.getImgPath());

            message.editMessage(emb.build()).queue();
            lastQuestionWasGuess = true;
        }

        private void answerQuestion(byte answer) {
            try {
                JSONObject json = new JSONObject(client.newCall(new Request.Builder()
                        .get()
                        .url(Strings.buildQueryUrl(ANSWER_URL,
                                "session", session,
                                "signature", signature,
                                "step", str(stepInfo.getStepNum()),
                                "answer", Byte.toString(answer)))
                        .build()).execute().body().string());

                try {
                    stepInfo = new StepInfo(json);
                } catch (JSONException ignored) {
                    emb.setImage(null)
                            .clearFields()
                            .addField("Status", "Akinator ran out of questions.", false);
                    onFinish.run();
                }

                if (stepInfo.getProgression() > 90) {
                    presentGuess();
                } else {
                    presentNextQuestion();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void answerGuess(byte answer) {
            try {
                if (answer == 0) {
                    client.newCall(new Request.Builder()
                            .get()
                            .url(Strings.buildQueryUrl(CHOICE_URL,
                                    "session", session,
                                    "signature", signature,
                                    "step", str(stepInfo.getStepNum()),
                                    "element", guess.getId()))
                            .build()).execute().body().close();
                    onFinish.run();
                } else if (answer == 1) {
                    client.newCall(new Request.Builder()
                            .get()
                            .url(Strings.buildQueryUrl(EXCLUSION_URL,
                                    "session", session,
                                    "signature", signature,
                                    "step", str(stepInfo.getStepNum()),
                                    "forward_answer", Byte.toString(answer)))
                            .build()).execute().body().close();

                    lastQuestionWasGuess = false;
                    presentNextQuestion();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private class StepInfo {
            private String signature = "";
            private String session = "";
            private final String question;
            private final int stepNum;
            private final double progression;

            StepInfo(JSONObject json) {
                JSONObject params = json.getJSONObject("parameters");
                JSONObject info = params.has("step_information") ? params.getJSONObject("step_information") : params;
                question = info.getString("question");
                stepNum = info.getInt("step");
                progression = info.getDouble("progression");

                JSONObject identification = params.optJSONObject("identification");
                if (identification != null) {
                    signature = identification.getString("signature");
                    session = identification.getString("session");
                }
            }

            String getQuestion() {
                return question;
            }

            int getStepNum() {
                return stepNum;
            }

            String getSignature() {
                return signature;
            }

            String getSession() {
                return session;
            }

            double getProgression() {
                return progression;
            }
        }

        private class Guess {
            private final String id;
            private final String name;
            private final String desc;
            private final int ranking;
            private final String pseudo;
            private final String imgPath;

            Guess() throws IOException {
                JSONObject json = new JSONObject(client.newCall(new Request.Builder()
                        .get()
                        .url(Strings.buildQueryUrl(GET_GUESS_URL,
                                "session", session,
                                "signature", signature,
                                "step", str(stepInfo.getStepNum())))
                        .build()).execute().body().string());

                JSONObject character = json.getJSONObject("parameters")
                        .getJSONArray("elements")
                        .getJSONObject(0)
                        .getJSONObject("element");

                id = character.getString("id");
                name = character.getString("name");
                desc = character.getString("description");
                ranking = character.getInt("ranking");
                pseudo = character.getString("pseudo");
                imgPath = character.getString("absolute_picture_path");
            }

            public String getDesc() {
                return desc;
            }

            public String getImgPath() {
                return imgPath;
            }

            public String getName() {
                return name;
            }

            public String getPseudo() {
                return pseudo;
            }

            public int getRanking() {
                return ranking;
            }

            public String getId() {
                return id;
            }

            @Override
            public String toString() {
                return "**" + name + "**\n"
                        + desc + '\n'
                        + "Ranking as **#" + ranking + "**";
            }
        }
    }
}
