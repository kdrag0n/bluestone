package com.kdrag0n.bluestone;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.re2j.Pattern;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.kdrag0n.bluestone.modules.*;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.types.ModuleLoadEvent;
import com.kdrag0n.bluestone.types.Perm;
import com.kdrag0n.bluestone.util.*;
import com.kdrag0n.bluestone.annotations.EventHandler;
import com.kdrag0n.bluestone.errors.PassException;
import com.kdrag0n.bluestone.handlers.MessageWaitEventListener;
import com.kdrag0n.bluestone.handlers.RejectedExecHandlerImpl;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.RestAction;
import okhttp3.*;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;
import static net.dv8tion.jda.core.entities.Game.*;

public class Bot extends ShardedBot implements EventListener {
    // Logger
    public static final Logger logger = LoggerFactory.getLogger(Bot.class);

    // Full module list
    private static final List<Class<? extends Module>> MODULE_CLASSES = ImmutableList.of(
            AfkModule.class,
            CoreModule.class,
            CryptoCurrencyModule.class,
            EntertainmentModule.class,
            GameModule.class,
            GoogleModule.class,
            InfoModule.class,
            MiscModule.class,
            ModerationModule.class,
            MusicModule.class,
            OwnerModule.class,
            PokemonModule.class,
            PollModule.class,
            ReminderModule.class,
            ReplModule.class,
            StarboardModule.class,
            StatReporterModule.class,
            UtilityModule.class,
            WelcomeModule.class,
            WikiModule.class
    );

    // Constants
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    public static final JSONObject EMPTY_JSON_OBJECT = new JSONObject();
    public static final JSONArray EMPTY_JSON_ARRAY = new JSONArray();
    private static final Pattern GENERAL_MENTION_PATTERN = Pattern.compile("^<@[!&]?[0-9]{17,20}>\\s*");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    // Executors
    public static final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(8,
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("Bot BG-Task Thread %d")
                    .build());
    private static final ThreadPoolExecutor moduleEventExecutor = new ThreadPoolExecutor(4, 32, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("Bot Module-Event Pool Thread %d")
                    .build(),
            new RejectedExecHandlerImpl("Module-Event"));
    public static final ThreadPoolExecutor threadExecutor = new ThreadPoolExecutor(4, 85, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(72),
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("Bot Command-Exec Pool Thread %d")
                    .build(),
            new RejectedExecHandlerImpl("Command-Exec"));
    public final EventWaiter eventWaiter = new EventWaiter();
    public final Map<String, Command> commands = new HashMap<>();
    public final Map<String, Module> modules = new HashMap<>();
    private final Map<Class<? extends Event>, List<ExtraEvent>> extraEvents = new HashMap<>();
    public final OkHttpClient http = new OkHttpClient.Builder()
            .cache(new Cache(new File("data/http_cache"), 24000000000L))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    public long ownerId = -1;
    public String ownerTag;
    public User selfUser;
    private long selfId;
    private String selfMention;
    private String selfGuildMention;
    private TIntSet readyShards = new TIntHashSet();
    public final PrefixStore prefixStore;

    private static final RandomSelect<Game> gameSelector = new RandomSelect<Game>(50)
            .add(playing("with my buddies"))
            .add(playing("with bits and bytes"))
            .add(playing("World Domination"))
            .add(playing("with you"))
            .add(playing("with potatoes"))
            .add(playing("something"))
            .add(streaming("data", ""))
            .add(streaming("music", "https://www.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ"))
            .add(streaming("your tunes", "https://www.youtube.com/watch?v=zQJh0MWvccs"))
            .add(listening("you"))
            .add(watching("darkness"))
            .add(watching("streams"))
            .add(streaming("your face", "https://www.youtube.com/watch?v=IUjZtoCrpyA"))
            .add(listening("alone"))
            .add(streaming("Alone", "https://www.youtube.com/watch?v=YnwsMEabmSo"))
            .add(streaming("bits and bytes", "https://www.youtube.com/watch?v=N3ZMvqISfvY"))
            .add(listening("Rick Astley"))
            .add(streaming("only the very best", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
            .add(listening("those potatoes"))
            .add(playing("with my fellow shards"))
            .add(listening("the cries of my shards"))
            .add(listening("as the sun goes down"))
            .add(streaming("Monstercat", "https://www.twitch.tv/monstercat"))
            .add(watching("dem videos"))
            .add(watching("you in your sleep"))
            .add(watching("over you as I sleep"))
            .add(watching("the movement of electrons"))
            .add(playing("with some protons"))
            .add(listening("trigger-happy players"))
            .add(playing("Discord Hacker v39.2"))
            .add(playing("Discord Hacker v42.0"))
            .add(listening("Discordians"))
            .add(streaming("donations", "https://paypal.me/dragon5232"))
            .add(listening("my people"))
            .add(listening("my favorites"))
            .add(watching("my minions"))
            .add(watching("the chosen ones"))
            .add(watching("stars combust"))
            .add(watching("your demise"))
            .add(streaming("the supernova", "https://www.youtube.com/watch?v=5WXyCJ1w3Ks"))
            .add(listening("something"))
            .add(streaming("something", "https://www.youtube.com/watch?v=FM7MFYoylVs"))
            .add(watching("I am Cow"))
            .add(watching("you play"))
            .add(watching("for raids"))
            .add(playing("buffing before the raid"))
            .add(streaming("this sick action", "https://www.youtube.com/watch?v=tD6KJ7QtQH8"))
            .add(listening("memes"))
            .add(watching("memes"))
            .add(playing("memes")) // memes
            .add(watching("that dank vid"));

    static {
        scheduledExecutor.setMaximumPoolSize(16);
        scheduledExecutor.setKeepAliveTime(16L, TimeUnit.SECONDS);

        RestAction.DEFAULT_FAILURE = e -> {
            if (e instanceof InsufficientPermissionException) {
                InsufficientPermissionException exp = (InsufficientPermissionException) e;
                Permission perm = exp.getPermission();
                if (perm == Permission.MESSAGE_WRITE)
                    return;
            }

            logger.error("RestAction failure", e);
        };
    }

    public JSONObject getKeys() {
        return getConfig().getJSONObject("keys");
    }

    public Bot(ShardManager manager, JSONObject config) {
        super(manager, config);
        manager.addEventListener(this, eventWaiter);

        prefixStore = new PrefixStore(getPool(), getConfig().optString("default_prefix", "!"));
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            onMessageReceived((MessageReceivedEvent) event);
        } else if (event instanceof ReadyEvent) {
            onReady((ReadyEvent) event);
        } else if (event instanceof ShutdownEvent) {
            onShutdown((ShutdownEvent) event);
        }

        event.getJDA().getShardInfo().getShardId();
        dispatchModuleEvent(event);
    }

    private void dispatchModuleEvent(Event event) {
        for (Map.Entry<Class<? extends Event>, List<ExtraEvent>> entry : extraEvents.entrySet()) {
            Class<? extends Event> eventClass = entry.getKey();

            if (eventClass.isInstance(event)) {
                List<ExtraEvent> events = entry.getValue();

                for (ExtraEvent extraEvent : events) {
                    Runnable task = () -> {
                        try {
                            extraEvent.getMethod().invoke(extraEvent.getParent(), event);
                        } catch (IllegalAccessException e) {
                            logger.error("Error dispatching {} to {}: handler not public",
                                    event.getClass().getSimpleName(),
                                    extraEvent.getMethod().getDeclaringClass().getName(), e);
                        } catch (InvocationTargetException eContainer) {
                            Throwable e = eContainer.getCause();

                            logger.error("{} error handling {}", extraEvent.getMethod().getDeclaringClass().getName(),
                                    event.getClass().getSimpleName(), e);
                        }
                    };

                    moduleEventExecutor.execute(task);
                }
            }
        }
    }

    private void updateOwnerInfo() {
        User owner;
        ApplicationInfo appInfo = manager.getShardById(0).asBot().getApplicationInfo().complete();
        owner = appInfo.getOwner();

        ownerId = owner.getIdLong();
        ownerTag = Module.getTag(owner);
    }

    private void onReady(ReadyEvent event) {
        JDA jda = event.getJDA();
        int shardId = Bot.getShardNum(jda) - 1;

        if (readyShards.contains(shardId))
            return;

        try {
            jda.getPresence().setStatus(OnlineStatus.ONLINE);

            if (readyShards.isEmpty()) {
                readyShards.add(shardId);

                selfUser = jda.getSelfUser();
                selfId = selfUser.getIdLong();
                selfMention = selfUser.getAsMention();
                selfGuildMention = "<@!" + selfId + '>';

                if (ownerId == -1)
                    updateOwnerInfo();

                for (Class<? extends Module> moduleClass : MODULE_CLASSES) {
                    try {
                        Module module = moduleClass.getConstructor(Bot.class).newInstance(this);
                        loadModule(module);
                    } catch (Throwable e) {
                        logger.error("Failed to register module {}", moduleClass.getName(), e);
                    }
                }

                dispatchModuleEvent(new ModuleLoadEvent(this));
            }

            if (jda.getGuildById(110373943822540800L) != null)
                Emotes.setHasDbots();

            if (jda.getGuildById(250780048943087618L) != null)
                Emotes.setHasParadise();

            Runnable task = () -> jda.getPresence().setGame(gameSelector.select());
            scheduledExecutor.scheduleAtFixedRate(task, 10, 120, TimeUnit.SECONDS);

        } finally {
            readyShards.add(shardId);

            if (Bot.getShardNum(jda) == manager.getShardsTotal()) {
                logger.info("Ready - ID {}", selfId);
            }
        }
    }

    private void loadModule(Module module) {
        Class<? extends Module> clazz = module.getClass();

        logger.info("Loading module {}...", clazz.getSimpleName());

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(com.kdrag0n.bluestone.annotations.Command.class)) {
                com.kdrag0n.bluestone.annotations.Command anno = method
                        .getDeclaredAnnotation(com.kdrag0n.bluestone.annotations.Command.class);

                List<Perm> perms = new ArrayList<>(method.getDeclaredAnnotations().length - 1);
                for (Annotation a : method.getDeclaredAnnotations()) {
                    Class<? extends Annotation> type = a.annotationType();
                    if (type == com.kdrag0n.bluestone.annotations.Command.class)
                        continue;

                    if (type == Perm.Owner.class) {
                        perms.add(Perm.BOT_OWNER);
                    } else {
                        Method valueMethod;
                        try {
                            valueMethod = type.getDeclaredMethod("value");
                        } catch (NoSuchMethodException ignored) {
                            continue;
                        }

                        Perm perm;

                        try {
                            perm = (Perm) valueMethod.invoke(a);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }

                        perms.add(perm);
                    }
                }

                Command command = new Command(anno.name(), anno.desc(), anno.usage(), anno.hidden(),
                        perms, anno.guildOnly(), anno.aliases(), method, module);

                if (commands.containsKey(command.name))
                    throw new IllegalStateException("Command '" + command.name + "' already registered!");
                else
                    commands.put(command.name, command);

                for (String al : command.aliases) {
                    if (commands.containsKey(al))
                        throw new IllegalStateException("Command '" + al + "' already registered!");
                    else
                        commands.put(al, command);
                }
            } else if (method.isAnnotationPresent(EventHandler.class)) {
                ExtraEvent extraEvent = new ExtraEvent(method, module);
                Class eventClass = method.getParameterTypes()[0];

                if (extraEvents.containsKey(eventClass)) {
                    extraEvents.get(eventClass).add(extraEvent);
                } else {
                    List<ExtraEvent> list = new ArrayList<>();
                    list.add(extraEvent);

                    extraEvents.put(eventClass, list);
                }
            }
        }

        modules.put(module.getName(), module);
    }

    private void onShutdown(ShutdownEvent event) {
        synchronized (this) {
            notifyAll();
        }
    }

    private void onMessageReceived(MessageReceivedEvent event) {
        final User author = event.getAuthor();

        if (author.isBot() ||
                author.getIdLong() == selfId ||
                !readyShards.contains(Bot.getShardNum(event.getJDA()) - 1))
            return;

        final Message message = event.getMessage();
        final String prefix;
        if (message.getGuild() == null) {
            prefix = prefixStore.defaultPrefix;
        } else {
            prefix = prefixStore.getPrefix(message.getGuild().getIdLong());
        }
        final String content = message.getContentRaw();
        final MessageChannel channel = event.getChannel();

        if (content == null) {
            return; // embeds maybe
        }

        if (content.startsWith(prefix)) {
            final String[] split = WHITESPACE_PATTERN.split(content.substring(prefix.length()), 0);
            if (split.length == 0)
                return;
            final ArrayListView args = new ArrayListView(split);

            final String cmdName = split[0].toLowerCase();

            if (commands.containsKey(cmdName)) {
                final Command command = commands.get(cmdName);

                command.simpleInvoke(this, event, args, prefix, cmdName, message.getContentRaw(), true);
            }
        } else if (content.startsWith(selfMention) || content.startsWith(selfGuildMention)) {
            final String request = Strings.renderMessage(message, message.getGuild(),
                    GENERAL_MENTION_PATTERN.matcher(message.getContentRaw()).replaceFirst(""));

            if (message.getGuild() != null &&
                    request.regionMatches(true, 0, "prefix", 0, 6 /* "prefix" */)) {
                // invoke the command
                final String[] split = WHITESPACE_PATTERN.split(content, 0);
                final String[] splitCopy = Arrays.copyOfRange(split, 1, split.length); // ignore the mention - 1st element
                final ArrayListView args = new ArrayListView(splitCopy); // ignore the command - 2nd element

                final Command command = commands.get("prefix");
                command.simpleInvoke(this, event, args, prefix, "prefix", // if the requested prefix is an @mention
                        GENERAL_MENTION_PATTERN.matcher(message.getContentRaw()).replaceFirst(""), false);
            } else if (request.length() > 0) {
                chatResponse(channel, "gbot_" + author.getId(), request, null);
            } else {
                channel.sendMessage("To talk, start your message with `@Goldmine`.\n" + "Prefix: `"
                        + Context.filterMessage(prefix) + '`').queue();
            }
        } else if (channel instanceof PrivateChannel && content.length() != 0 && content.charAt(0) != '`') {
            final String request = Strings.renderMessage(message, null, message.getContentRaw());
            chatResponse(channel, "bs_GMdbot2-" + author.getId(), request, "💬 ");
        }
    }

    private void chatResponse(MessageChannel channel, String sessionID, String query, String respPrefix) {
        String reqDest = getConfig().optString("chatengine_url", null);
        if (reqDest == null) {
            channel.sendMessage("My owner hasn't set up ChatEngine yet.").queue();
            return;
        }
        channel.sendTyping().queue();

        http.newCall(new Request.Builder()
                .post(RequestBody.create(JSON_MEDIA_TYPE,
                        new JSONObject().put("session", sessionID).put("query", query).toString()))
                .url(reqDest).header("Authorization", getKeys().optString("chatengine")).build())
                .enqueue(Bot.callback(response -> {
                    JSONObject resp = new JSONObject(response.body().string());

                    if (!resp.optBoolean("success", false)) {
                        logger.error("ChatEngine returned error: {}", resp.optString("error", "Not specified"));
                        channel.sendMessage(Emotes.getFailure() + " An error occurred getting a response!").queue();
                        return;
                    }

                    String toSend;
                    if (respPrefix == null)
                        toSend = resp.getString("response");
                    else
                        toSend = respPrefix + resp.getString("response");

                    channel.sendMessage(Context.filterMessage(toSend)).queue(null, e -> {
                    });
                }, e -> {
                    logger.error("Error getting ChatEngine response", e);
                    channel.sendMessage(Emotes.getFailure() + " Try again later.").queue(null, ex -> {
                    });
                }));
    }

    public Message waitForMessage(JDA jda, long millis, Predicate<Message> check) {
        AtomicReference<Message> lock = new AtomicReference<>();
        MessageWaitEventListener listener = new MessageWaitEventListener(lock, check);
        jda.addEventListener(listener);

        synchronized (lock) {
            try {
                lock.wait(millis);
            } catch (InterruptedException e) {
                jda.removeEventListener(listener);
                return null;
            }
            return lock.get();
        }
    }

    public String formatUptime() {
        return Strings.formatDuration((new Date().getTime() - startTime.getTime()) / 1000L);
    }

    public int getTrackCount() {
        MusicModule module = (MusicModule) modules.get("Music");
        if (module == null)
            return 0;

        return module.getTracksLoaded();
    }

    public int getStreamCount() {
        MusicModule module = (MusicModule) modules.get("Music");
        if (module == null)
            return 0;

        return module.getActiveStreamCount();
    }

    public static int getShardNum(JDA jda) {
        JDA.ShardInfo sInfo = jda.getShardInfo();
        if (sInfo == null)
            return 1;
        else
            return sInfo.getShardId() + 1;
    }

    public static int start(String token, int shardCount, JSONObject config) {
        logger.info("Starting bot...");

        if (shardCount < 1) {
            logger.info("At least 1 shard is required; {} requested", shardCount);
            return 1;
        } else if (shardCount == 2) {
            logger.error("2 shards not supported");
            return 1;
        }

        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
                .setToken(token)
                .setShardsTotal(shardCount)
                .setWebsocketFactory(new WebSocketFactory()
                        .setConnectionTimeout(120000))
                .setBulkDeleteSplittingEnabled(false)
                .setHttpClientBuilder(new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true))
                .setStatus(OnlineStatus.IDLE)
                .setGame(Game.playing("starting up..."));

        if ((System.getProperty("os.arch").startsWith("x86") || System.getProperty("os.arch").equals("amd64"))
                && (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX))
            builder.setAudioSendFactory(new NativeAudioSendFactory());

        ShardManager manager;
        try {
            manager = builder.build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }

        for (int shardId = 0; shardId < shardCount; shardId++)
            manager.start(shardId);

        Bot bot = new Bot(manager, config);
        synchronized (bot) {
            try {
                bot.wait();
            } catch (InterruptedException ignored) {}
        }

        return 0;
    }

    public interface EConsumer<T> {
        void accept(T value) throws Throwable;
    }

    public static okhttp3.Callback callback(EConsumer<Response> success, EConsumer<Throwable> failure) {
        return new okhttp3.Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    if (!(response.isSuccessful() || response.isRedirect())) {
                        failure.accept(new IOException(
                                "Request unsuccessful, status " + response.code() + " " + response.message()));
                        return;
                    }

                    if (response.isRedirect()) {
                        logger.warn("Response is redirect, status " + response.code() + " " + response.message()
                                + ". Destination: " + val(response.header("Location")).or("[not sent]"));
                    }

                    success.accept(response);
                } catch (Throwable e) {
                    try {
                        if (!(e instanceof PassException))
                            logger.error("Error in HTTP success callback", e);

                        failure.accept(e);
                    } catch (Throwable ee) {
                        if (ee instanceof PermissionException) {
                            PermissionException pe = (PermissionException) ee;
                            if (pe.getPermission() != Permission.MESSAGE_WRITE) {
                                logger.error("Permission error in HTTP fail callback after success callback error", pe);
                            }
                        } else {
                            logger.error("Error running HTTP call failure callback after error in success callback!",
                                    ee);
                        }
                    }
                } finally {
                    ResponseBody body = response.body();
                    if (body != null)
                        body.close();
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                try {
                    failure.accept(e);
                } catch (Throwable ee) {
                    logger.error("Error running HTTP call failure callback!", ee);
                }
            }
        };
    }
}
