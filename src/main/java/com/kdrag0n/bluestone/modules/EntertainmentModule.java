package com.kdrag0n.bluestone.modules;

import com.google.common.collect.ImmutableList;
import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.emotes.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.types.Perm;
import com.kdrag0n.bluestone.util.StreamUtil;
import com.kdrag0n.bluestone.util.Strings;
import com.kdrag0n.bluestone.util.UnicodeString;
import gnu.trove.map.TCharObjectMap;
import gnu.trove.map.hash.TCharObjectHashMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.WordUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.Reference;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;
import static java.lang.String.format;

public class EntertainmentModule extends Module {
    private static final Logger logger = LoggerFactory.getLogger(EntertainmentModule.class);
    private static final Map<String, UnicodeString> charsets = new HashMap<String, UnicodeString>() {
        {
            put("normal", uniString(
                    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("fullwidth", uniString(
                    "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ０１２３４５６７８９～ ｀！＠＃＄％＾＆＊（）－＿＝＋［］｛｝|；：＇＂,＜．＞/？"));
            put("circled", uniString(
                    "ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ0①②③④⑤⑥⑦⑧⑨~ `!@#$%^&⊛()⊖_⊜⊕[]{}⦶;:'\",⧀⨀⧁⊘?⦸"));
            put("circled_inverse", uniString(
                    "🅐🅑🅒🅓🅔🅕🅖🅗🅘🅙🅚🅛🅜🅝🅞🅟🅠🅡🅢🅣🅤🅥🅦🅧🅨🅩🅐🅑🅒🅓🅔🅕🅖🅗🅘🅙🅚🅛🅜🅝🅞🅟🅠🅡🅢🅣🅤🅥🅦🅧🅨🅩⓿123456789~ `!@#$%^&⊛()⊖_⊜⊕[]{}⦶;:'\",⧀⨀⧁⊘?⦸"));
            put("bold", uniString(
                    "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("bold_old", uniString(
                    "𝖆𝖇𝖈𝖉𝖊𝖋𝖌𝖍𝖎𝖏𝖐𝖑𝖒𝖓𝖔𝖕𝖖𝖗𝖘𝖙𝖚𝖛𝖜𝖝𝖞𝖟𝕬𝕭𝕮𝕯𝕰𝕱𝕲𝕳𝕴𝕵𝕶𝕷𝕸𝕹𝕺𝕻𝕼𝕽𝕾𝕿𝖀𝖁𝖂𝖃𝖄𝖅0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("bold_italic", uniString(
                    "𝒂𝒃𝒄𝒅𝒆𝒇𝒈𝒉𝒊𝒋𝒌𝒍𝒎𝒏𝒐𝒑𝒒𝒓𝒔𝒕𝒖𝒗𝒘𝒙𝒚𝒛𝑨𝑩𝑪𝑫𝑬𝑭𝑮𝑯𝑰𝑱𝑲𝑳𝑴𝑵𝑶𝑷𝑸𝑹𝑺𝑻𝑼𝑽𝑾𝑿𝒀𝒁0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("bold_script", uniString(
                    "𝓪𝓫𝓬𝓭𝓮𝓯𝓰𝓱𝓲𝓳𝓴𝓵𝓶𝓷𝓸𝓹𝓺𝓻𝓼𝓽𝓾𝓿𝔀𝔁𝔂𝔃𝓐𝓑𝓒𝓓𝓔𝓕𝓖𝓗𝓘𝓙𝓚𝓛𝓜𝓝𝓞𝓟𝓠𝓡𝓢𝓣𝓤𝓥𝓦𝓧𝓨𝓩0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("double", uniString(
                    "𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("mono", uniString(
                    "𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉𝟶𝟷𝟸𝟹𝟺𝟻𝟼𝟽𝟾𝟿~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("sans", uniString(
                    "𝖺𝖻𝖼𝖽𝖾𝖿𝗀𝗁𝗂𝗃𝗄𝗅𝗆𝗇𝗈𝗉𝗊𝗋𝗌𝗍𝗎𝗏𝗐𝗑𝗒𝗓𝖠𝖡𝖢𝖣𝖤𝖥𝖦𝖧𝖨𝖩𝖪𝖫𝖬𝖭𝖮𝖯𝖰𝖱𝖲𝖳𝖴𝖵𝖶𝖷𝖸𝖹𝟢𝟣𝟤𝟥𝟦𝟧𝟨𝟩𝟪𝟫~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("sans_bold", uniString(
                    "𝗮𝗯𝗰𝗱𝗲𝗳𝗴𝗵𝗶𝗷𝗸𝗹𝗺𝗻𝗼𝗽𝗾𝗿𝘀𝘁𝘂𝘃𝘄𝘅𝘆𝘇𝗔𝗕𝗖𝗗𝗘𝗙𝗚𝗛𝗜𝗝𝗞𝗟𝗠𝗡𝗢𝗣𝗤𝗥𝗦𝗧𝗨𝗩𝗪𝗫𝗬𝗭𝟬𝟭𝟮𝟯𝟰𝟱𝟲𝟳𝟴𝟵~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("sans_bold_italic", uniString(
                    "𝙖𝙗𝙘𝙙𝙚𝙛𝙜𝙝𝙞𝙟𝙠𝙡𝙢𝙣𝙤𝙥𝙦𝙧𝙨𝙩𝙪𝙫𝙬𝙭𝙮𝙯𝘼𝘽𝘾𝘿𝙀𝙁𝙂𝙃𝙄𝙅𝙆𝙇𝙈𝙉𝙊𝙋𝙌𝙍𝙎𝙏𝙐𝙑𝙒𝙓𝙔𝙕0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("sans_italic", uniString(
                    "𝘢𝘣𝘤𝘥𝘦𝘧𝘨𝘩𝘪𝘫𝘬𝘭𝘮𝘯𝘰𝘱𝘲𝘳𝘴𝘵𝘶𝘷𝘸𝘹𝘺𝘻𝘈𝘉𝘊𝘋𝘌𝘍𝘎𝘏𝘐𝘑𝘒𝘓𝘔𝘕𝘖𝘗𝘘𝘙𝘚𝘛𝘜𝘝𝘞𝘟𝘠𝘡0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("parenthesized", uniString(
                    "⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵0⑴⑵⑶⑷⑸⑹⑺⑻⑼~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("regional", uniString(
                    "🇦🇧🇨🇩🇪🇫🇬🇭🇮🇯🇰🇱🇲🇳🇴🇵🇶🇷🇸🇹🇺🇻🇼🇽🇾🇿🇦🇧🇨🇩🇪🇫🇬🇭🇮🇯🇰🇱🇲🇳🇴🇵🇶🇷🇸🇹🇺🇻🇼🇽🇾🇿0123456789~ `!@#$%^&*()-_=+[]{}|;:'\",<.>/?"));
            put("squared", uniString(
                    "🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉0123456789~ `!@#$%^&⧆()⊟_=⊞[]{}|;:'\",<⊡>⧄?⧅"));
            put("upside_down", uniString(
                    "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀qƆpƎℲפHIſʞ˥WNOԀQɹS┴∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86~ ,¡@#$%^⅋*)(-‾=+][}{|;:,,,'>˙</¿"));
            put("why_not", uniString(
                    "å∫ç∂´ƒ©˙ˆ∆˚¬µ˜øπœ®ß†¨√∑≈¥ΩÅıÇÎÉÏöÓÎ╲⎝╱ÂõØ∏∑çÍìÜêÂ⧸Ú⎠º¡™£¢∞§¶•ª `⁄€‹›ﬁﬂ‡°·‚–⧹≠±“‘”’»…ÚæÆ≤¯≥˘ÚæÆ?"));
        }
    };
    private static final int[] normalChars = { 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
            112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77,
            78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 126, 32, 96, 33,
            64, 35, 36, 37, 94, 38, 42, 40, 41, 45, 95, 61, 43, 91, 93, 123, 125, 124, 59, 58, 39, 34, 44, 60, 46, 62,
            47, 63 };
    @SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
    private static final TCharObjectMap<String> alphabetToEmote = new TCharObjectHashMap<String>() {
        {
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
        }
    };
    private static final String[] ADJECTIVES = { "lovingly", "lamely", "limply", "officially", "for money", "sadly",
            "roughly", "angrily", "harshly", "without hesitation", "quickly", "greedily", "shamefully", "dreadfully",
            "painfully", "intensely", "digitally", "unofficially", "nervously", "invitingly", "seductively",
            "embarassingly", "thoroughly", "doubtfully", "proudly" };
    private static final String[] FIGHTS = { "pokes %s with a spear", "impales %s", "stabs %s",
            "guts %s with a stone knife", "eviscerates %s with a sharp stone", "decapitates %s with a wand",
            "fires cruise missle at %s", "backstabs %s", "punches %s", "poisons %s", "opens trapdoor under %s",
            "360 quick scopes %s", "noscopes %s", "normally snipes %s", "uses katana to slice through %s",
            "deadily stares at %s", "uses a trebuchet to shoot a 95kg projectile over 300 meters at %s",
            "snaps neck from %s", "pours lava over %s", "dumps acid above %s", "shoots with a glock 17 at %s",
            "incinerates %s", "uses a tridagger to stab %s", "assasinates %s", "fires with a minigun at %s",
            "fires with bazooka at %s", "uses granny bomb at %s", "throws bananabomb at %s",
            "throws holy grenade at %s" };
    private static final String[] DEATHS = { "%s dies.", "%s survives.", "Blood pours from %s.", "%s heals themself.",
            "Fairies take %s away.", "An old man carries %s away.", "%s is in shock.", "%s passes out." };

    private static final List<ImmutablePair<Pattern, Integer>> MEME_PATTERNS = ImmutableList
            .of(pair("(one does not simply) (.*)", 61579), pair("(i don'?t always .*) (but when i do,? .*)", 61532),
                    pair("aliens ()(.*)", 101470), pair("grumpy cat ()(.*)", 405658),
                    pair("(.*),? (\\1 everywhere)", 347390), pair("(not sure if .*) (or .*)", 61520),
                    pair("(y u no) (.+)", 61527), pair("(brace yoursel[^\\s]+) (.*)", 61546),
                    pair("(.*) (all the .*)", 61533), pair("(.*) (that would be great|that'?d be great)", 563423),
                    pair("(.*) (\\w+\\stoo damn .*)", 61580), pair("(yo dawg .*) (so .*)", 101716),
                    pair("(.*) (.* gonna have a bad time)", 100951),
                    pair("(am i the only one around here) (.*)", 259680), pair("(what if i told you) (.*)", 100947),
                    pair("(.*) (ain'?t nobody got time for? that)", 442575), pair("(.*) (i guarantee it)", 10672255),
                    pair("(.*) (a+n+d+ it'?s gone)", 766986), pair("(.* bats an eye) (.* loses their minds?)", 1790995),
                    pair("(back in my day) (.*)", 718432))
            .stream().map(p -> ImmutablePair.of(Pattern.compile(p.getLeft(), Pattern.CASE_INSENSITIVE), p.getRight()))
            .collect(Collectors.toList());

    private static final String[] EIGHT_BALL_CHOICES = {"Yes, definitely!",
            "Of course!",
            "Yes!",
            "Probably.",
            "Hmm, I'm not sure...",
            "I'm not sure...",
            "I don't think so.",
            "Hmm, I don't really think so.",
            "Definitely not.",
            "No.",
            "Probably not.",
            "Sure!",
            "Try again later...",
            "I don't know.",
            "Maybe...",
            "Yes, of course!",
            "No, probably not."};

    private static final Color BLEACH_COLOR = new Color(51, 143, 216);
    private static final MessageEmbed BLEACH_EMBED = new EmbedBuilder().setColor(BLEACH_COLOR).setTitle("Bleach")
            .setImage("https://upload.wikimedia.org/wikipedia/commons/d/d3/Clorox_Bleach_products.jpg").build();
    private final EmoteProviderManager emoteProviderManager = new EmoteProviderManager();

    private static UnicodeString uniString(String javaString) {
        return new UnicodeString(javaString);
    }

    private static <L, R> ImmutablePair<L, R> pair(L l, R r) {
        return ImmutablePair.of(l, r);
    }

    public EntertainmentModule(Bot bot) {
        super(bot);

        OkHttpClient http = new OkHttpClient();
        emoteProviderManager.addProvider(new TwitchEmoteProvider(http));
        emoteProviderManager.addProvider(new BetterTTVEmoteProvider(http));
        emoteProviderManager.addProvider(new FrankerFaceZEmoteProvider(http));
        emoteProviderManager.addProvider(new DiscordEmoteProvider());
    }

    public String getName() {
        return "Entertainment";
    }

    @Command(name = "reverse", desc = "Reverse some text.", usage = "[text]")
    public void cmdReverse(Context ctx) {
        ctx.send(":repeat: " + StringUtils.reverse(ctx.rawArgs)).queue();
    }

    @Command(name = "emotisay", desc = "Show some text using Emoji block letters.", aliases = { "emotesay",
            "esay" }, usage = "[text]")
    public void cmdEmotisay(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("You need some text!");
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

    @Command(name = "bleach", desc = "Clorox bleach.")
    public void cmdBleach(Context ctx) {
        ctx.send(BLEACH_EMBED).queue();
    }

    @Command(name = "cat", desc = "Get a random cat!", aliases = { "randcat" })
    public void cmdCat(Context ctx) {
        ctx.channel.sendTyping().queue();

        try {
            String cat = new JSONObject(
                    bot.http.newCall(new Request.Builder().get().url("https://aws.random.cat/meow").build()).execute()
                            .body().string()).optString("file", null);
            String fact = new JSONObject(
                    bot.http.newCall(new Request.Builder().get().url("https://catfact.ninja/fact").build()).execute()
                            .body().string()).optString("fact", null);

            if (cat == null || fact == null) {
                ctx.fail("Couldn't get a cat!");
                return;
            }

            Color color;
            if (ctx.guild == null)
                color = randomColor();
            else
                color = val(ctx.member.getColor()).or(Color.WHITE);

            ctx.send(new EmbedBuilder().setImage(cat).setColor(color)
                    .setAuthor("Random Cat", null, "https://khronodragon.com/cat.png")
                    .addField("Did You Know?", fact, false).build()).queue();
        } catch (IOException ignored) {
            ctx.fail("Failed to get a cat!");
        }
    }

    @Command(name = "dog", desc = "Get a random dog!", aliases = { "randdog" })
    public void cmdDog(Context ctx) {
        ctx.channel.sendTyping().queue();

        try {
            String cat = new JSONObject(
                    bot.http.newCall(new Request.Builder().get().url("https://dog.ceo/api/breeds/image/random").build())
                            .execute().body().string()).optString("message", null);
            String fact = val(new JSONObject(bot.http
                    .newCall(new Request.Builder().get().url("https://dog-api.kinduff.com/api/facts?number=1").build())
                    .execute().body().string()).optJSONArray("facts")).or(JSONArray::new).optString(0, null);

            if (cat == null || fact == null) {
                ctx.fail("Couldn't get a dog!");
                return;
            }

            Color color;
            if (ctx.guild == null)
                color = randomColor();
            else
                color = val(ctx.member.getColor()).or(Color.WHITE);

            ctx.send(new EmbedBuilder().setImage(cat).setColor(color)
                    .setAuthor("Random Dog", null, "https://khronodragon.com/dog.png")
                    .addField("Did You Know?", fact, false).build()).queue();
        } catch (IOException ignored) {
            ctx.fail("Failed to get a dog!");
        }
    }

    @Command(name = "emote", desc = "Get an emoticon, from many sources.", usage = "[emote name]")
    public void cmdEmote(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail(
                    "You need to specify an emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.");
            return;
        }

        String eName = ctx.args.get(0);
        if (eName.equalsIgnoreCase("add")) {
            cmdAddEmote(ctx);
            return;
        }

        final String url = emoteProviderManager.getFirstUrl(eName);
        if (url == null) {
            ctx.fail("No such emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.");
            return;
        }
        EmoteInfo info = emoteProviderManager.getFirstInfo(eName);

        bot.http.newCall(new Request.Builder().get().url(url).build()).enqueue(Bot.callback(response -> {
            Message msg = null;

            if (info.description != null && info.description.length() > 0) {
                msg = new MessageBuilder().append(info.description).build();
            }

            InputStream stream = response.body().byteStream();
            if (msg == null)
                ctx.channel.sendFile(stream, "emote.png").queue(unused -> response.body().close());
            else
                ctx.channel.sendMessage(msg).addFile(stream, "emote.png").queue();

        }, e -> ctx.send(Emotes.getFailure() + " Failed to fetch emote.").queue(), false));
    }

    @Perm.ManageEmotes
    @Command(name = "add_emote", desc = "Add an emote to the server.", usage = "[emote name]", aliases = { "addemote",
            "emoteadd", "emote_add", "+emote", "+e" }, guildOnly = true)
    public void cmdAddEmote(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail(
                    "You need to specify an emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.");
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_EMOTES)) {
            ctx.fail("I need to be able to **manage emotes**!");
            return;
        }

        String eName = ctx.args.get(0);
        if (eName.equalsIgnoreCase("add") && ctx.args.length > 1)
            eName = ctx.args.get(1);
        final String n = eName;

        final String url = emoteProviderManager.getFirstUrl(eName);
        if (url == null) {
            ctx.fail("No such emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV are supported.");
            return;
        }

        ctx.channel.sendTyping().queue();

        bot.http.newCall(new Request.Builder().get().url(url).build()).enqueue(Bot.callback(response -> {
            final String nm = url.startsWith("https://cdn.discordapp.com/emojis/")
                    ? emoteProviderManager.getFirstInfo(n).name
                    : n;

            InputStream is = response.body().byteStream();
            Emote emote = ctx.guild.createEmote(nm, Icon.from(is))
                    .reason("Adding emote " + nm + " by user request. User has Manage Emotes.").complete();

            ctx.send(Emotes.getSuccess() + " Emote added. " + emote.getAsMention()).queue();
        }, e -> ctx.send(Emotes.getFailure() + " Failed to fetch or create emote.").queue()));
    }

    private String applyStyle(String orig, UnicodeString mapTo) {
        UnicodeString mapFrom = charsets.get("normal");
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

    @Command(name = "styles", desc = "List the available text styles.", aliases = { "fonts" })
    public void cmdStyles(Context ctx) {
        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor("Text Styles", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl()).setColor(randomColor())
                .setDescription(Strings.EMPTY)
                .setTimestamp(Instant.now());

        for (Map.Entry<String, UnicodeString> entry : charsets.entrySet()) {
            emb.appendDescription("    \u2022 " + applyStyle(entry.getKey(), entry.getValue()) + "\n");
        }
        emb.appendDescription("\n\nUse a style with the `style` command: `style [name] [text]`.");

        ctx.send(emb.build()).queue();
    }

    @Command(name = "style", desc = "Apply a style to some text.", aliases = { "font" })
    public void cmdStyle(Context ctx) {
        if (ctx.args.empty) {
            ctx.send(Emotes.getFailure() + " Usage is `style [style name] [text]`.\n"
                    + "\nTip: *use the `styles` command to see what there is.*").queue();
            return;
        }
        if (ctx.args.length < 2) {
            ctx.fail("Usage is `style [style name] [text]`.");
            return;
        }

        String styleName = ctx.args.get(0);
        if (!charsets.containsKey(styleName)) {
            ctx.fail("No such style! List them with the `styles` command.");
            return;
        }

        String text = ctx.rawArgs.substring(styleName.length()).trim();
        ctx.send(applyStyle(text, charsets.get(styleName))).queue();
    }

    @Command(name = "lmgtfy", desc = "Let me Google that for you!")
    public void cmdLmgtfy(Context ctx) throws UnsupportedEncodingException {
        if (ctx.args.empty) {
            ctx.fail("I need some search terms!");
            return;
        }

        String query = URLEncoder.encode(ctx.args.join(' '), "UTF-8");
        ctx.send("<https://lmgtfy.com/?q=" + query + '>').queue();
    }

    @Command(name = "slap", desc = "Slap someone with passion.", aliases = { "boop", "poke", "hit" })
    public void cmdSlap(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need someone to " + ctx.invoker + '!');
            return;
        }

        ctx.send(format("%s %ss %s %s.", (ctx.guild == null ? ctx.author : ctx.member).getAsMention(),
                ctx.invoker, ctx.rawArgs, randomChoice(ADJECTIVES))).queue();
    }

    @Command(name = "meme", desc = "Generate a custom meme.", usage = "[meme text / [top text] | [bottom text]]")
    public void cmdMeme(Context ctx) {
        if (ctx.rawArgs.length() < 2) {
            ctx.fail("I need some text to use!");
            return;
        }
        ctx.channel.sendTyping().queue();

        FormBody.Builder data = new FormBody.Builder();

        int template = -1024;
        String topText = null;
        String bottomText = null;
        for (ImmutablePair<Pattern, Integer> pair : MEME_PATTERNS) {
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
            } else {
                String[] results = ArrayUtils
                        .subarray(StringUtils.split(WordUtils.wrap(StringUtils.replaceChars(ctx.rawArgs, '\n', ' '),
                                ctx.rawArgs.length() / 2 + 1, "\n", true, "\\s+"), '\n'), 0, 2);

                topText = results[0];
                bottomText = results.length > 1 ? results[1] : "";
            }
        }

        data.add("template_id", Integer.toUnsignedString(template));
        try {
            data.add("username", bot.getKeys().getJSONObject("imgflip").getString("username"));
            data.add("password", bot.getKeys().getJSONObject("imgflip").getString("password"));
        } catch (JSONException none) {
            data.add("username", "imgflip_hubot");
            data.add("password", "imgflip_hubot");
        }
        data.add("text0", topText);
        data.add("text1", bottomText);

        bot.http.newCall(new Request.Builder().post(data.build()).url("https://api.imgflip.com/caption_image").build())
                .enqueue(Bot.callback(response -> {
                    JSONObject resp = new JSONObject(response.body().string());

                    if (resp.optBoolean("success", false)) {
                        ctx.send(new EmbedBuilder().setColor(randomColor())
                                .setImage(resp.getJSONObject("data").getString("url")).build()).queue();
                    } else {
                        ctx.send(Emotes.getFailure() + " Error: `" + resp.getString("error_message") + '`').queue();
                    }
                }, e -> {
                    logger.error("Imgflip request errored", e);
                    ctx.send(Emotes.getFailure() + " Request failed. `" + e.getMessage() + '`').queue();
                }));
    }

    @Command(name = "attack", desc = "Hurt someone with determination.", aliases = { "stab", "kill", "punch", "shoot",
            "hurt", "fight" })
    public void cmdAttack(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need someone to " + ctx.invoker + '!');
            return;
        }
        final String target = ctx.rawArgs;

        ctx.send((ctx.guild == null ? ctx.author : ctx.member).getAsMention() + ' '
                + format(randomChoice(FIGHTS), target) + ". " + format(randomChoice(DEATHS), target)).queue();
    }

    @Command(name = "soon", desc = "Feel the speed of Soon™.", aliases = { "soontm" })
    public void cmdSoon(Context ctx) {
        ctx.channel.sendFile(EntertainmentModule.class.getResourceAsStream("/assets/soon.gif"), "soon.gif")
                .queue();
    }

    @Command(name = "flip", desc = "Flip a coin.", aliases = {"coinflip"})
    public void cmdFlip(Context ctx) {
        ctx.send("The coin toss revealed... **" + (getRandInt(0, 1) == 1 ? "heads" : "tails") + "**!").queue();
    }

    @Command(name = "roll", desc = "Roll a virtual dice.", aliases = {"dice"})
    public void cmdRoll(Context ctx) {
        ctx.send("I rolled a **" + getRandInt(1, 7) + "**.").queue();
    }

    @Command(name = "8ball", desc = "A magic 8 ball!", aliases = {"8"})
    public void cmd8Ball(Context ctx) {
        if (ctx.args.empty)
            ctx.fail("I need a question!");
        else
            ctx.send("🔮 " + randomChoice(EIGHT_BALL_CHOICES)).queue();
    }
}
