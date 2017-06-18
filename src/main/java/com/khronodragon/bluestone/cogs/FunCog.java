package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.emotes.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.util.Strings;
import com.khronodragon.bluestone.util.UnisafeString;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class FunCog extends Cog {
    private static final EmoteProviderManager EMOTE_PROVIDER_MANAGER = new EmoteProviderManager();
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
    private static final Map<Character, String> alphabetToEmote = new HashMap<Character, String>() {{
        put(' ', "    ");
        put('#', ":hash:");
        put('!', ":exclamation:");
        put('?', ":question:");
        put('$', ":heavy_dollar_sign:");
        put('-', ":heavy_minus_sign:");
        put('.', ":small_blue_diamond:");
        put('~', ":wavy_dash:");
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
        put('^', ":arrow_up:");
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

    private static final UnisafeString uniString(String javaString) {
        return new UnisafeString(javaString);
    }

    public FunCog(Bot bot) {
        super(bot);
        EMOTE_PROVIDER_MANAGER.addProvider(new TwitchEmoteProvider());
        EMOTE_PROVIDER_MANAGER.addProvider(new BetterTTVEmoteProvider());
        EMOTE_PROVIDER_MANAGER.addProvider(new FrankerFaceZEmoteProvider());
        EMOTE_PROVIDER_MANAGER.addProvider(new DiscordEmoteProvider());
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
            ctx.send(":warning: You need some text!").queue();
            return;
        }

        ctx.send(Strings.simpleJoin(ctx.rawArgs.chars().mapToObj(c -> {
            final Character character = (Character) (char) c;

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
        ctx.send(new EmbedBuilder()
                .setColor(randomColor())
                .setTitle("Bleach")
                .setImage("https://upload.wikimedia.org/wikipedia/commons/d/d3/Clorox_Bleach_products.jpg")
                .build()).queue();
    }

    @Command(name = "cat", desc = "Get a random cat!")
    public void cmdCat(Context ctx) {
        Unirest.get("http://random.cat/meow")
                .asJsonAsync(new Callback<JsonNode>() {
                    public void completed(HttpResponse<JsonNode> response) {
                        String imageUrl = response.getBody().getObject().getString("file");
                        if (imageUrl == null) {
                            ctx.send(":warning: Couldn't get a cat!").queue();
                        } else {
                            ctx.send(new EmbedBuilder()
                                    .setImage(imageUrl)
                                    .setColor(randomColor())
                                    .build()).queue();
                        }
                    }

                    public void failed(UnirestException e) {
                        ctx.send(":warning: Failed to get a cat!").queue();
                    }

                    public void cancelled() {
                        ctx.send(":x: The request was cancelled!").queue();
                    }
                });
    }

    @Command(name = "emote", desc = "Get an emoticon, from many sources.", usage = "[emote name]")
    public void cmdEmote(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: You need to specify an emote!").queue();
            return;
        }
        if (!EMOTE_PROVIDER_MANAGER.isFullyLoaded()) {
            ctx.send(":x: The emote data hasn't been loaded yet! Try again soon.").queue();
            return;
        }

        final String url = EMOTE_PROVIDER_MANAGER.getFirstUrl(ctx.rawArgs);
        if (url == null) {
            ctx.send(":warning: No such emote! Twitch, Discord (custom only), FrankerFaceZ, and BetterTTV should work.").queue();
            return;
        }
        EmoteInfo info = EMOTE_PROVIDER_MANAGER.getFirstInfo(ctx.rawArgs);

        Unirest.get(url)
                .asBinaryAsync(new Callback<InputStream>() {
                    @Override
                    public void completed(HttpResponse<InputStream> response) {
                        Message msg = null;
                        if (info.description != null) {
                            msg = new MessageBuilder()
                                    .append(info.description)
                                    .build();
                        }
                        ctx.channel.sendFile(response.getBody(), "emote.png", msg).queue();
                    }

                    @Override
                    public void failed(UnirestException e) {
                        ctx.send(":warning: Failed to fetch emote.").queue();
                    }

                    @Override
                    public void cancelled() {
                        ctx.send(":x: The request was cancelled.").queue();
                    }
                });
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

        return newString.toString().replace("_", "\\_");
    }

    @Command(name = "styles", desc = "List the available text styles.", aliases = {"fonts"})
    public void cmdStyles(Context ctx) {
        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor("Text Styles", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setColor(randomColor())
                .setDescription("\u200b");

        for (Map.Entry<String, UnisafeString> entry: charsets.entrySet()) {
            emb.appendDescription("    \u2022 " + applyStyle(entry.getKey(), entry.getValue()) + "\n");
        }
        emb.appendDescription("\n\nUse a style with the `style` command: `style [name] [text]`.");

        ctx.send(emb.build()).queue();
    }

    @Command(name = "style", desc = "Apply a style to some text.", aliases = {"font"})
    public void cmdStyle(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: Usage is `style [style name] [text]`.\n" +
                    "\nTip: *use the `styles` command to see what there is.*").queue();
            return;
        }
        if (ctx.args.size() < 2) {
            ctx.send(":warning: Usage is `style [style name] [text]`.").queue();
            return;
        }

        String styleName = ctx.args.get(0);
        if (!charsets.containsKey(styleName)) {
            ctx.send(":warning: No such style! List them with the `styles` command.").queue();
            return;
        }

        String text = ctx.rawArgs.substring(styleName.length()).trim();
        ctx.send(applyStyle(text, charsets.get(styleName))).queue();
    }

    @Command(name = "lmgtfy", desc = "Let me Google that for you!")
    public void cmdLmgtfy(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need some search terms!").queue();
            return;
        }

        ctx.send("<http://lmgtfy.com/?q=" + ctx.args.stream()
                .map(s -> s.replace("+", "%2B"))
                .collect(Collectors.joining("+")) + ">").queue();
    }

    @Command(name = "slap", desc = "Slap someone, with passion.", aliases = {"boop", "poke", "hit"})
    public void cmdSlap(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need someone to " + ctx.invoker + "!").queue();
            return;
        }

        ctx.send(format("{0} {1}s *{2}* **{3}**.", (ctx.guild == null ? ctx.author : ctx.member).getAsMention(),
                ctx.invoker, ctx.rawArgs, randomChoice(ADJECTIVES))).queue();
    }

    @Command(name = "attack", desc = "Hurt someone, with determination.", aliases = {"stab", "kill", "punch", "shoot", "hurt", "fight"})
    public void cmdAttack(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need someone to " + ctx.invoker + "!").queue();
            return;
        }
        final String target = format("*{0}}*", ctx.rawArgs);

        ctx.send(format("{0} {1}. {2}", (ctx.guild == null ? ctx.author : ctx.member).getAsMention(),
                format(randomChoice(FIGHTS), target), format(randomChoice(DEATHS), target))).queue();
    }

    @Command(name = "charlie", desc = "Ask a question... Charlie Charlie are you there?")
    public void cmdCharlie(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need a question!").queue();
            return;
        }
        String question = ctx.rawArgs.endsWith("?") ? ctx.rawArgs : ctx.rawArgs + "?";

        ctx.send(format("*Charlie Charlie* {0}\n**{1}**", question, (randint(0, 1) == 1 ? "Yes" : "No"))).queue();
    }

    @Command(name = "soon", desc = "Feel the loading of 10000 years, aka Soon™.", aliases = {"soontm"})
    public void cmdSoon(Context ctx) {
        ctx.channel.sendFile(FunCog.class.getResourceAsStream("/assets/soon.gif"), "soon.gif", null).queue();
    }
}
