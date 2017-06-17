package com.khronodragon.bluestone;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import com.khronodragon.bluestone.errors.CheckFailure;
import com.khronodragon.bluestone.errors.GuildOnlyError;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.errors.PermissionError;
import com.khronodragon.bluestone.handlers.MessageWaitEventListener;
import com.khronodragon.bluestone.handlers.ReactionWaitEventListener;
import com.khronodragon.bluestone.handlers.RejectedExecHandlerImpl;
import com.khronodragon.bluestone.util.ClassUtilities;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.JDA.ShardInfo;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import javax.security.auth.login.LoginException;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import static java.text.MessageFormat.format;

public class Bot extends ListenerAdapter implements ClassUtilities {
    public static final String USER_AGENT = "Goldmine/2 Discord Bot (tiny.cc/goldbot)";
    public Logger logger = LogManager.getLogger(Bot.class);
    private ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                                                            .setDaemon(true)
                                                            .setNameFormat("Bot BG-Task Thread %d")
                                                            .build());
    private ThreadPoolExecutor cogEventExecutor = new ThreadPoolExecutor(5, 50, 45, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(30), new ThreadFactoryBuilder()
                                                .setDaemon(true)
                                                .setNameFormat("Bot Cog-Event Pool Thread %d")
                                                .build(), new RejectedExecHandlerImpl("Cog-Event"));
    public ThreadPoolExecutor threadExecutor = new ThreadPoolExecutor(3, 85, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(60), new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Bot Command-Exec Pool Thread %d")
            .build(), new RejectedExecHandlerImpl("Command-Exec"));
    public Date startTime = new Date();
    private HashSet<ScheduledFuture> tasks = new HashSet<>();
    public HashMap<String, Command> commands = new HashMap<>();
    public HashMap<String, Cog> cogs = new HashMap<>();
    public HashSet<EventedCog> eventedCogs = new HashSet<>();
    public HashMap<String, AtomicInteger> commandCalls = new HashMap<>();
    public ApplicationInfo appInfo;
    public User owner;
    public final DataStore store;
    private JDA jda;
    private ShardUtil shardUtil;

    public JsonObject getConfig() {
        return config;
    }

    public void setConfig(JsonObject config) {
        this.config = config;
    }

    private JsonObject config;

    public JsonObject getKeys() {
        return config.getAsJsonObject("keys");
    }

    public Bot() {
        super();
        scheduledExecutor.setMaximumPoolSize(6);
        scheduledExecutor.setKeepAliveTime(16L, TimeUnit.SECONDS);
        store = new DataStore();
    }

    public void setJda(JDA jda) {
        this.jda = jda;
        final ShardInfo sInfo = jda.getShardInfo();
        if (sInfo != null) {
            logger = LogManager.getLogger("Bot [" + sInfo.getShardString() + ']');
        }
    }

    public JDA getJda() {
        return jda;
    }

    public ScheduledThreadPoolExecutor getScheduledExecutor() {
        return scheduledExecutor;
    }

    public void setShardUtil(ShardUtil util) {
        shardUtil = util;
    }

    public ShardUtil getShardUtil() {
        return shardUtil;
    }

    private static void sprint(String text) {
        System.out.println(text);
    }

    public int getShardNum() {
        ShardInfo sInfo = jda.getShardInfo();
        if (sInfo == null) {
            return 1;
        } else {
            return sInfo.getShardId() + 1;
        }
    }

    public int getShardTotal() {
        ShardInfo sInfo = jda.getShardInfo();
        if (sInfo == null) {
            return 1;
        } else {
            return sInfo.getShardTotal();
        }
    }

    public static String vagueTrace(Throwable e) {
        StackTraceElement[] elements = e.getStackTrace();
        StackTraceElement[] limitedElems = {elements[0], elements[1]};
        List<String> stack = new ArrayList<>();
        stack.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        for (StackTraceElement elem: limitedElems) {
            String base = "> " + elem.getClassName() + '.' + elem.getMethodName();
            base += elem.isNativeMethod() ? "(native)" : format("({0})", elem.getLineNumber());
            stack.add(base);
        }
        return String.join("\n  ", stack);
    }

    public static String renderStackTrace(Throwable e) {
        StackTraceElement[] elements = e.getStackTrace();
        List<String> stack = new ArrayList<>();
        stack.add(e.getClass().getName() + ": " + e.getMessage());
        for (StackTraceElement elem: elements) {
            String base = "at " + elem.toString();
            stack.add(base);
        }
        return StringUtils.join(stack, "\n    ");
    }

    @Override
    public void onGenericEvent(Event event) {
        for (EventedCog cog: eventedCogs) {
            Runnable task = () -> cog.onEvent(event);

            if (cog.needsThreadedEvents()) {
                cogEventExecutor.execute(task);
            } else {
                task.run();
            }
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        JDA jda = event.getJDA();
        long uid = jda.getSelfUser().getIdLong();

        if (jda.getSelfUser().isBot()) {
            this.appInfo = jda.asBot().getApplicationInfo().complete();
            this.owner = appInfo.getOwner();
        } else {
            this.owner = jda.getSelfUser();
        }
        logger.info("Ready - ID {}", uid);

        Runnable task = () -> {
            String statusLine;
            switch (ThreadLocalRandom.current().nextInt(1, 12)) {
                case 1:
                    statusLine = format("with {0} users", shardUtil.getUserCount());
                    break;
                case 2:
                    statusLine = format("in {0} channels", shardUtil.getChannelCount());
                    break;
                case 3:
                    statusLine = format("in {0} servers", shardUtil.getGuildCount());
                    break;
                case 4:
                    statusLine = format("in {0} guilds", shardUtil.getGuildCount());
                    break;
                case 5:
                    statusLine = format("from shard {0} of {0}", getShardNum(), getShardTotal());
                    break;
                case 6:
                    statusLine = "with my buddies";
                    break;
                case 7:
                    statusLine = "with bits and bytes";
                    break;
                case 8:
                    statusLine = "World Domination";
                    break;
                case 9:
                    statusLine = "with you";
                    break;
                case 10:
                    statusLine = "with potatoes";
                    break;
                case 11:
                    statusLine = "something";
                    break;
                default:
                    statusLine = "severe ERROR!";
                    break;
            }

            jda.getPresence().setGame(Game.of(statusLine));
        };

        ScheduledFuture future = scheduledExecutor.scheduleAtFixedRate(task, 10, 75, TimeUnit.SECONDS);
        tasks.add(future);

        Reflections reflector = new Reflections("com.khronodragon.bluestone.cogs");
        Set<Class<? extends Cog>> cogClasses = reflector.getSubTypesOf(Cog.class);
        for (Class cogClass: cogClasses) {
            try {
                Object obj = cogClass.getConstructor(this.getClass()).newInstance(this);
                ((Cog) obj).register();
            } catch (NoSuchMethodException|InstantiationException|IllegalAccessException|InvocationTargetException e) {
                logger.error("Failed to register cog {}", cogClass.getName(), e);
            }
        }
    }

    @Override
    public void onResume(ResumedEvent event) {
        logger.info("WebSocket resumed.");
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        logger.info("Reconnected.");
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        logger.info("Shutting down...");
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void onException(ExceptionEvent event) {
        logger.error("Error", event.getCause());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        JDA jda = event.getJDA();
        User author = event.getAuthor();

        if (author.isBot())
            return;
        if (author.getIdLong() == jda.getSelfUser().getIdLong())
            return;

        String prefix = ")";
        Message message = event.getMessage();
        String content = message.getRawContent();
        MessageChannel channel = event.getChannel();

        if (content.startsWith(prefix)) {
            ArrayList<String> args = new ArrayList<>(Arrays.asList(content.split("\\s+")));
            String cmdName = args.get(0).substring(prefix.length()).toLowerCase();
            args.remove(0);

            if (commands.containsKey(cmdName)) {
                Command command = commands.get(cmdName);
                try {
                    command.invoke(this, event, args, prefix, cmdName);
                } catch (IllegalAccessException e) {
                    logger.error("Severe command ({}) invocation error:", cmdName, e);
                    channel.sendMessage(":x: A severe internal error occurred.").queue();
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause == null) {
                        logger.error("Unknown command ({}) invocation error:", cmdName, e);
                        channel.sendMessage(":x: An unknown internal error occurred.").queue();
                    } else if (cause instanceof PassException) {
                    } else {
                        logger.error("Command ({}) invocation error:", cmdName, cause);
                        channel.sendMessage(format(":warning: Error in `{0}{1}`:```java\n{2}```", prefix, cmdName, vagueTrace(cause))).queue();
                    }
                } catch (PermissionError e) {
                    channel.sendMessage(format("{0} Not enough permissions for `{1}{2}`! **{3}** will work.", author.getAsMention(), prefix, cmdName,
                            Strings.smartJoin(command.getFriendlyPerms()))).queue();
                } catch (GuildOnlyError e) {
                    channel.sendMessage("Sorry, that command only works in a guild.").queue();
                } catch (CheckFailure e) {
                    logger.error("Checks failed for command {}:", cmdName);
                    channel.sendMessage(format("{0} A check for `{1}{2}` failed. Do you not have permissions?", author.getAsMention(), prefix, cmdName)).queue();
                } catch (Exception e) {
                    logger.error("Unknown command ({}) error:", cmdName, e);
                    channel.sendMessage(":x: A severe internal error occurred.").queue();
                }

                if (commandCalls.containsKey(command.name)) {
                    commandCalls.get(command.name).incrementAndGet();
                } else {
                    commandCalls.put(command.name, new AtomicInteger(1));
                }
            }
        }
    }

    public Message waitForMessage(long millis, Predicate<Message> check) {
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

    public MessageReactionAddEvent waitForReaction(long millis, Predicate<MessageReactionAddEvent> check) {
        AtomicReference<MessageReactionAddEvent> lock = new AtomicReference<>();
        ReactionWaitEventListener listener = new ReactionWaitEventListener(lock, check);
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

    public boolean isSelfbot() {
        return !jda.getSelfUser().isBot();
    }

    long getUptimeMillis() {
        return new Date().getTime() - startTime.getTime();
    }

    public String formatUptime() {
        return formatDuration(getUptimeMillis() / 1000L);
    }

    public static String formatMemory() {
        Runtime runtime = Runtime.getRuntime();
        NumberFormat format = NumberFormat.getInstance();
        return format.format((runtime.totalMemory() - runtime.freeMemory()) / 1048576.0f) + " MB";
    }

    public static String formatDuration(long duration) {
        if (duration == 9223372036854775L) { // Long.MAX_VALUE / 1000L
            return "[unknown]";
        }

        long h = duration / 3600;
        long m = (duration % 3600) / 60;
        long s = duration % 60;
        long d = h / 24;
        h = h % 24;
        String sd = (d > 0 ? String.valueOf(d) + ' ' + "day" + (d == 1 ? "" : "s") : "");
        String sh = (h > 0 ? String.valueOf(h) + ' ' + "hr" : "");
        String sm = (m < 10 && m > 0 && h > 0 ? "0" : "") + (m > 0 ? (h > 0 && s == 0 ? String.valueOf(m) : String.valueOf(m) + ' ' + "min") : "");
        String ss = (s == 0 && (h > 0 || m > 0) ? "" : (s < 10 && (h > 0 || m > 0) ? "0" : "") + String.valueOf(s) + ' ' + "sec");
        return sd + (d > 0 ? " " : "") + sh + (h > 0 ? " " : "") + sm + (m > 0 ? " " : "") + ss;
    }

    public static int start(String token, int shardCount, AccountType accountType, JsonObject config) throws LoginException, RateLimitedException {
        System.out.println("Starting...");

        if (shardCount < 1) {
            System.out.println("There needs to be at least 1 shard, or how will the bot work?");
            return 1;
        } else if (shardCount == 2) {
            System.out.println("2 shards is very buggy and doesn't work well. Use either 1 or 3+ shards.");
            return 1;
        }

        ShardUtil shardUtil = new ShardUtil(shardCount);

        IntStream.range(0, shardCount).forEach(shardId -> {
            Runnable monitor = () -> {
                final Logger logger = LogManager.getLogger("ShardMonitor " + shardId);
                while (true) {
                    Bot bot = new Bot();
                    bot.setConfig(config);
                    JDABuilder builder = new JDABuilder(accountType)
                            .setToken(token)
                            .addEventListener(bot)
                            .setAudioEnabled(true)
                            .setAutoReconnect(true)
                            .setWebSocketTimeout(120000)
                            .setBulkDeleteSplittingEnabled(false)
                            .setStatus(OnlineStatus.ONLINE)
                            .setGame(Game.of("something"));

                    if (shardCount != 1) {
                        builder.useSharding(shardId, shardCount);
                    }

                    JDA jda;
                    try {
                        jda = builder.buildAsync();
                    } catch (Exception e) {
                        logger.error("Failed to log in.", e);
                        if (shardCount == 1) {
                            System.exit(1);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {}
                        continue;
                    }
                    bot.setJda(jda);
                    shardUtil.setShard(shardId, bot);
                    bot.setShardUtil(shardUtil);

                    synchronized (bot) {
                        try {
                            bot.wait();
                        } catch (InterruptedException e) {
                            while (jda.getStatus() == JDA.Status.CONNECTED) {
                                try {
                                    Thread.sleep(25);
                                } catch (InterruptedException ex) {
                                    try {
                                        Thread.sleep(15000);
                                    } catch (InterruptedException exx) {}
                                }
                            }
                        }
                    }

                    if (jda.getStatus() != JDA.Status.DISCONNECTED) {
                        if (jda.getStatus() == JDA.Status.CONNECTED) {
                            jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
                        }
                        jda.shutdown(false);
                    }
                    if (shardCount == 1) {
                        System.exit(0);
                    }

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {}
                }
            };

            Thread monThread = new Thread(monitor, "Bot Shard-" + shardId + " Monitor Thread");
            monThread.start();
            try {
                Thread.sleep(5100);
            } catch (InterruptedException e) {}
        });

        return 0;
    }
}
