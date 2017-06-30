package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.text.MessageFormat.format;

public class ReplCog extends Cog {
    private static final Logger logger = LogManager.getLogger(ReplCog.class);
    public static final String GROOVY_PRE_INJECT = "import net.dv8tion.jda.core.entities.*\n" +
            "import net.dv8tion.jda.core.*\n" +
            "import net.dv8tion.jda.core.entities.impl.*\n" +
            "import net.dv8tion.jda.core.audio.*\n" +
            "import net.dv8tion.jda.core.audit.*\n" +
            "import net.dv8tion.jda.core.managers.*\n" +
            "import net.dv8tion.jda.core.exceptions.*\n" +
            "import net.dv8tion.jda.core.events.*\n" +
            "import net.dv8tion.jda.core.utils.*\n" +
            "import com.khronodragon.bluestone.*\n" +
            "import org.apache.logging.log4j.*\n" +
            "import javax.script.*\n" +
            "import com.khronodragon.bluestone.cogs.*\n" +
            "import com.khronodragon.bluestone.errors.*\n" +
            "import org.json.*\n" +
            "import com.khronodragon.bluestone.sql.*\n" +
            "import com.khronodragon.bluestone.handlers.*\n" +
            "import com.khronodragon.bluestone.enums.*\n" +
            "import com.khronodragon.bluestone.util.*\n" +
            "import java.time.*\n";
    private static final String PYTHON_IMPORTS = "from net.dv8tion.jda.core.entities import AudioChannel, Channel, ChannelType, EmbedType, Emote, EntityBuilder, Game, Guild, GuildVoiceState, Icon, IFakeable, IMentionable, Invite, IPermissionHolder, ISnowflake, Member, Message, MessageChannel, MessageEmbed, MessageHistory, MessageReaction, MessageType, PermissionOverride, PrivateChannel, Role, SelfUser, TextChannel, User, VoiceChannel, VoiceState, Webhook\n" +
            "from net.dv8tion.jda.core import AccountType, EmbedBuilder, JDA, JDABuilder, JDAInfo, MessageBuilder, OnlineStatus, Permission, Region\n" +
            "from net.dv8tion.jda.core.entities.impl import AbstractChannelImpl, EmoteImpl, GameImpl, GuildImpl, GuildVoiceStateImpl, InviteImpl, JDAImpl, MemberImpl, MessageEmbedImpl, MessageImpl, PermissionOverrideImpl, PrivateChannelImpl, RoleImpl, SelfUserImpl, TextChannelImpl, UserImpl, VoiceChannelImpl, WebhookImpl\n" +
            "from net.dv8tion.jda.core.audio import AudioConnection, AudioPacket, AudioReceiveHandler, AudioSendHandler, AudioWebSocket, CombinedAudio, Decoder, UserAudio\n" +
            "from net.dv8tion.jda.core.audit import ActionType, AuditLogChange, AuditLogEntry, AuditLogKey, AuditLogOption, TargetType\n" +
            "from net.dv8tion.jda.core.managers import AccountManager, AccountManagerUpdatable, AudioManager, ChannelManager, ChannelManagerUpdatable, GuildController, GuildManager, GuildManagerUpdatable, PermOverrideManager, PermOverrideManagerUpdatable, Presence, RoleManager, RoleManagerUpdatable, WebhookManager, WebhookManagerUpdatable\n" +
            "from net.dv8tion.jda.core.exceptions import AccountTypeException, ErrorResponseException, GuildUnavailableException, PermissionException, RateLimitedException\n" +
            "from net.dv8tion.jda.core.events import DisconnectEvent, Event, ExceptionEvent, ReadyEvent, ReconnectedEvent, ResumedEvent, ShutdownEvent, StatusChangeEvent\n" +
            "from net.dv8tion.jda.core.utils import IOUtil, MiscUtil, NativeUtil, PermissionUtil, SimpleLog, WidgetUtil\n" +
            "from com.khronodragon.bluestone import Bot, Cog, Command, Context, DataStore, Emotes, ExtraEvent, Permissions, PrefixStore, ShardUtil, Start\n" +
            "from org.apache.logging.log4j import CloseableThreadContext, EventLogger, Level, Logger, Marker, MarkerManager, ThreadContext, LoggingException, LogManager\n" +
            "from javax.script import ScriptEngineManager, ScriptEngine\n" +
            "from com.khronodragon.bluestone.cogs import AdminCog, CogmanCog, CoreCog, FunCog, GoogleCog, KewlCog, LuckCog, ModerationCog, MusicCog, OwnerCog, PokemonCog, QuotesCog, ReplCog, StatReporterCog, UtilityCog, WebCog, WelcomeCog\n" +
            "from com.khronodragon.bluestone.errors import CheckFailure, GuildOnlyError, MessageException, PassException, PermissionError, UserNotFound\n" +
            "from org.json import CDL, Cookie, CookieList, HTTP, HTTPTokener, JSONArray, JSONException, JSONML, JSONObject, JSONPointer, JSONPointerException, JSONString, JSONStringer, JSONTokener, JSONWriter, Property, XML, XMLTokener\n" +
            "from com.khronodragon.bluestone.sql import BotAdmin, GuildPrefix, GuildWelcomeMessages, Quote\n" +
            "from com.khronodragon.bluestone.handlers import MessageWaitEventListener, ReactionWaitEventListener, RejectedExecHandlerImpl\n" +
            "from com.khronodragon.bluestone.enums import BucketType, MessageDestination\n" +
            "from com.khronodragon.bluestone.util import Base65536, ClassUtilities, EqualitySet, IntegerZeroTypeAdapter, MinecraftUtil, NullValueWrapper, Paginator, RegexUtil, StreamUtils, Strftime, StringMapper, StringReplacerCallback, Strings, UnisafeString\n" +
            "from com.khronodragon.bluestone.pokemon import Ability, Description, EggGroup, Evolution, Move, Pokemon, Sprite, Type\n" +
            "from com.khronodragon.bluestone.emotes import BetterTTVEmoteProvider, DiscordEmoteProvider, EmoteInfo, EmoteProvider, EmoteProviderManager, FrankerFaceZEmoteProvider, TwitchEmoteProvider\n" +
            "from org.apache.logging.log4j import CloseableThreadContext, EventLogger, Level, Logger, Marker, MarkerManager, ThreadContext, LoggingException, LogManager\n" +
            "from java.time import Clock, DateTimeException, DayOfWeek, Duration, Instant, LocalDate, LocalDateTime, LocalTime, Month, MonthDay, OffsetDateTime, OffsetTime, Period, Ser, Year, YearMonth, ZonedDateTime, ZoneId, ZoneOffset, ZoneRegion\n" +
            "from java.util import HashMap, HashSet, LinkedHashSet, LinkedHashMap, TreeSet, Set, List, Map, Optional, ArrayList, LinkedList, TreeMap, ConcurrentHashMap, Date, Base64, AbstractList, AbstractMap, Collection, AbstractCollection, IdentityHashMap, Random\n" +
            "from java.lang import System, Long, Integer, Character, String, Short, Byte, Boolean";

    private TLongSet replSessions = new TLongHashSet();

    public ReplCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "REPL";
    }
    public String getDescription() {
        return "A multilingual REPL, in Discord!";
    }

    public static String cleanupCode(String code) {
        String stage1 = code.replaceFirst("```(?:js|javascript|py|python|java|groovy|scala|kotlin|kt|lua|ruby|rb)\n?", "");
        return StringUtils.stripEnd(StringUtils.stripStart(stage1, "`"), "`");
    }

    @Command(name = "repl", desc = "A multilingual REPL, in Discord!", perms = {"owner"}, usage = "[language] {flags}", thread=true)
    public void cmdRepl(Context ctx) {
        if (ctx.args.size() < 1) {
            ctx.send("You need to specify a language, like `scala` or `js`!").queue();
            return;
        }

        String prefix = "`";
        String language = ctx.args.get(0);
        ScriptEngineManager man = new ScriptEngineManager();

        if (language.equalsIgnoreCase("list")) {
            List<ScriptEngineFactory> factories = man.getEngineFactories();
            List<String> langs = new ArrayList<>();

            for (ScriptEngineFactory factory: factories) {
                langs.add(format("{0} {1} ({2} {3})", factory.getEngineName(), factory.getEngineVersion(),
                        factory.getLanguageName(), factory.getLanguageVersion()));
            }

            ctx.send("List of available languages:\n    \u2022 " + StringUtils.join(langs, "\n    \u2022 ")).queue();
            return;
        }

        ScriptEngine engine = man.getEngineByName(language.toLowerCase());
        if (engine == null) {
            ctx.send(Emotes.getFailure() + " Invalid REPL language!").queue();
            return;
        }

        if (replSessions.contains(ctx.channel.getIdLong())) {
            ctx.send("Already running a REPL session in this channel. Exit it with `quit`.").queue();
            return;
        }
        replSessions.add(ctx.channel.getIdLong());

        engine.put("ctx", ctx);
        engine.put("context", ctx);
        engine.put("bot", ctx.bot);
        engine.put("last", null);
        engine.put("jda", ctx.jda);
        engine.put("message", ctx.message);
        engine.put("author", ctx.author);
        engine.put("channel", ctx.channel);
        engine.put("guild", ctx.guild);
        engine.put("test", "Test right back at ya!");
        engine.put("msg", ctx.message);
        if (language.equalsIgnoreCase("python"))
            engine.put("imports", PYTHON_IMPORTS);

        ctx.send("REPL started. Prefix is " + prefix).queue();
        while (true) {
            Message response = bot.waitForMessage(0, msg -> msg.getAuthor().getIdLong() == ctx.author.getIdLong() &&
                    msg.getChannel().getIdLong() == ctx.channel.getIdLong() &&
                    msg.getRawContent().startsWith(prefix));
            engine.put("message", response);
            engine.put("msg", response);

            String cleaned = cleanupCode(response.getRawContent());

            if (stringExists(cleaned, "quit", "exit", "System.exit()", "System.exit", "System.exit(0)", "exit()")) {
                ctx.send("**Exiting...**").queue();
                replSessions.remove(ctx.channel.getIdLong());
                break;
            }

            Object result;
            try {
                if (language.equalsIgnoreCase("groovy"))
                    result = GROOVY_PRE_INJECT + engine.eval(cleaned);
                else
                    result = engine.eval(cleaned);
            } catch (ScriptException e) {
                result = e.getCause();
                if (result instanceof ScriptException) {
                    result = ((ScriptException) result).getCause();
                }
            } catch (Throwable e) {
                logger.warn("Error executing code in REPL", e);
                result = Bot.renderStackTrace(e);
            }
            if (result instanceof RestAction)
                result = ((RestAction) result).complete();
            engine.put("last", result);

            if (result != null) {
                try {
                    String strResult = result.toString();
                    if (language.equals("groovy") && strResult.startsWith(GROOVY_PRE_INJECT))
                        strResult = strResult.substring(GROOVY_PRE_INJECT.length());

                    ctx.send("```java\n" + strResult + "```").queue();
                } catch (Exception e) {
                    logger.warn("Error sending message in REPL", e);
                    try {
                        ctx.send("```java\n" + bot.renderStackTrace(e) + "```").queue();
                    } catch (Exception ex) {
                        logger.error("Error reporting send error in REPL", ex);
                    }
                }
            } else {
                response.addReaction("✅").queue();
            }
        }
    }
}