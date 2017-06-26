package com.khronodragon.bluestone;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.khronodragon.bluestone.errors.CheckFailure;
import com.khronodragon.bluestone.errors.GuildOnlyError;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.errors.PermissionError;
import com.khronodragon.bluestone.handlers.MessageWaitEventListener;
import com.khronodragon.bluestone.handlers.ReactionWaitEventListener;
import com.khronodragon.bluestone.handlers.RejectedExecHandlerImpl;
import com.khronodragon.bluestone.sql.BotAdmin;
import com.khronodragon.bluestone.sql.GuildPrefix;
import com.khronodragon.bluestone.util.ClassUtilities;
import com.khronodragon.bluestone.util.Strings;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.JDA.ShardInfo;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.reflections.Reflections;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static com.khronodragon.bluestone.util.Strings.str;
import static java.text.MessageFormat.format;

public class Bot extends ListenerAdapter implements ClassUtilities {
    public static final String USER_AGENT = "Goldmine/2 Discord Bot (tiny.cc/goldbot)";
    public Logger logger = LogManager.getLogger(Bot.class);
    private ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                                                            .setDaemon(true)
                                                            .setNameFormat("Bot BG-Task Thread %d")
                                                            .build());
    private ThreadPoolExecutor cogEventExecutor = new ThreadPoolExecutor(5, 50, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(30), new ThreadFactoryBuilder()
                                                .setDaemon(true)
                                                .setNameFormat("Bot Cog-Event Pool Thread %d")
                                                .build(), new RejectedExecHandlerImpl("Cog-Event"));
    public ThreadPoolExecutor threadExecutor = new ThreadPoolExecutor(3, 85, 10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(60), new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("Bot Command-Exec Pool Thread %d")
            .build(), new RejectedExecHandlerImpl("Command-Exec"));
    public Date startTime = new Date();
    private JDA jda;
    private ShardUtil shardUtil;
    private HashSet<ScheduledFuture> tasks = new HashSet<>();
    public HashMap<String, Command> commands = new HashMap<>();
    public HashMap<String, Cog> cogs = new HashMap<>();
    public HashSet<EventedCog> eventedCogs = new HashSet<>();
    public HashMap<String, AtomicInteger> commandCalls = new HashMap<>();
    public ApplicationInfo appInfo;
    public User owner;

    public Dao<BotAdmin, Long> getAdminDao() {
        return shardUtil.getAdminDao();
    }

    public Dao<GuildPrefix, Long> getPrefixDao() {
        return shardUtil.getPrefixStore().getDao();
    }

    public JSONObject getConfig() {
        return shardUtil.getConfig();
    }

    public JSONObject getKeys() {
        return getConfig().getJSONObject("keys");
    }

    public Bot() {
        super();

        scheduledExecutor.setMaximumPoolSize(6);
        scheduledExecutor.setKeepAliveTime(16L, TimeUnit.SECONDS);
    }

    public JdbcConnectionSource getDatabase() {
        return shardUtil.getDatabase();
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

    protected static String vagueTraceElement(StackTraceElement elem) {
        String base = "> " + StringUtils.replaceOnce(StringUtils.replaceOnce(elem.getClassName(),
                "java.util", "stdlib"),
                "com.khronodragon.bluestone", "bot") + '.' + elem.getMethodName();
        base += elem.isNativeMethod() ? "(native)" : format("({0})", elem.getLineNumber());

        return base;
    }

    public static String vagueTrace(Throwable e) {
        StackTraceElement[] elements = e.getStackTrace();
        StackTraceElement[] limitedElems = {elements[0], elements[1]};
        List<String> stack = new ArrayList<>();
        stack.add(e.getClass().getSimpleName() + ": " + e.getMessage());

        for (StackTraceElement elem: limitedElems) {
            stack.add(vagueTraceElement(elem));
        }

        if (!stack.stream()
            .anyMatch(s -> s.contains("> bot.cogs"))) {
            Optional<StackTraceElement> optElem = Arrays.stream(elements)
                    .filter(el -> el.getClassName().startsWith("com.khronodragon.bluestone.cogs."))
                    .findFirst();

            if (optElem.isPresent()) {
                stack.add("");
                stack.add(vagueTraceElement(optElem.get()));
            }
        }
        return String.join("\n\u2007\u2007", stack);
    }

    public static String renderStackTrace(Throwable e) {
        return renderStackTrace(e, "    ", "at ");
    }

    public static String renderStackTrace(Throwable e, String joinSpaces, String elemPrefix) {
        StackTraceElement[] elements = e.getStackTrace();
        List<String> stack = new LinkedList<>();
        stack.add(e.getClass().getName() + ": " + e.getMessage());

        for (StackTraceElement elem: elements) {
            String base = elemPrefix + elem.toString();
            stack.add(base);
        }

        return StringUtils.join(stack, '\n' + joinSpaces);
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
                Cog cog = (Cog) cogClass.getConstructor(Bot.class).newInstance(this);
                registerCog(cog);
                cog.load();
            } catch (Throwable e) {
                logger.error("Failed to register cog {}", cogClass.getName(), e);
            }
        }
    }

    public void registerCog(Cog cog) {
        Class clazz = cog.getClass();

        for (Method method: clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(com.khronodragon.bluestone.annotations.Command.class)) {
                com.khronodragon.bluestone.annotations.Command anno = method.getAnnotation(com.khronodragon.bluestone.annotations.Command.class);

                Command command = new Command(
                        anno.name(), anno.desc(), anno.usage(), anno.hidden(),
                        anno.perms(), anno.guildOnly(), anno.aliases(), method, cog,
                        anno.thread(), anno.reportErrors()
                );

                if (commands.containsKey(command.name))
                    throw new IllegalStateException("Command '" + command.name + "' already registered!");
                else
                    commands.put(command.name, command);

                for (String al: command.aliases) {
                    if (commands.containsKey(al))
                        throw new IllegalStateException("Command '" + al + "' already registered!");
                    else
                        commands.put(al, command);
                }
            }
        }

        cogs.put(cog.getName(), cog);

        if (cog instanceof EventedCog) {
            eventedCogs.add((EventedCog) cog);
        }
    }

    public void unregisterCog(Cog cog) {
        for (Map.Entry<String, Command> entry: new HashSet<>(commands.entrySet())) {
            Command cmd = entry.getValue();

            if (cmd.cog == cog) {
                commands.remove(entry.getKey());
            }
        }
        cog.unload();

        cogs.remove(cog.getName(), cog);
        if (cog instanceof EventedCog)
            eventedCogs.remove(cog);
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
        final JDA jda = event.getJDA();
        final User author = event.getAuthor();

        if (author.isBot())
            return;
        if (author.getIdLong() == jda.getSelfUser().getIdLong())
            return;

        final Message message = event.getMessage();
        final String prefix;
        if (message.getGuild() == null) {
            prefix = shardUtil.getPrefixStore().getDefaultPrefix();
        } else {
            prefix = shardUtil.getPrefixStore().getPrefix(message.getGuild().getIdLong());
        }
        final String content = message.getRawContent();
        final MessageChannel channel = event.getChannel();

        if (content.startsWith(prefix)) {
            String[] split = content.substring(prefix.length()).split("\\s+");
            List<String> args = new ArrayList<>(split.length - 1);

            for (int i = 1; i < split.length; i++)
                args.add(split[i]);

            String cmdName = split[0].toLowerCase();

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
                        // assume error has already been sent
                    } else {
                        logger.error("Command ({}) invocation error:", cmdName, cause);
                        channel.sendMessage(format(":warning: Error!```java\n{2}```This error will be reported.",
                                prefix, cmdName, vagueTrace(cause))).queue();

                        if (command.reportErrors)
                            reportErrorToOwner(cause, message, command);
                    }
                } catch (PermissionError e) {
                    channel.sendMessage(format("{0} Missing permission for `{1}{2}`! **{3}** will work.",
                            author.getAsMention(), prefix, cmdName,
                            Strings.smartJoin(command.getFriendlyPerms(), "or"))).queue();
                } catch (GuildOnlyError e) {
                    channel.sendMessage("Sorry, that command only works in a guild.").queue();
                } catch (CheckFailure e) {
                    channel.sendMessage(format("{0} A check for `{1}{2}` failed. Do you not have permissions?",
                            author.getAsMention(), prefix, cmdName)).queue();
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
        } else if (content.startsWith(message.getGuild() == null ?
                jda.getSelfUser().getAsMention() : message.getGuild().getSelfMember().getAsMention())) {
            if (content.substring(jda.getSelfUser().getAsMention().length())
                    .trim().equalsIgnoreCase("prefix")) {
                channel.sendMessage("My prefix here is `" + prefix + "`.").queue();
            } else {
                channel.sendMessage("Hey there! I can't talk to you yet. However, if you want my prefix, say `@" +
                        Cog.getTag(jda.getSelfUser()) + " prefix`!").queue();
            }
        }
    }

    public void reportErrorToOwner(Throwable e, Message msg, Command cmd) {
        owner.openPrivateChannel().queue(ch -> {
            ch.sendMessage(errorEmbed(e, msg, cmd)).queue();
        });
    }

    private static MessageEmbed errorEmbed(Throwable e, Message msg, Command cmd) {
        Date date = new Date();
        String stack = renderStackTrace(e, "\u3000", "> ");

        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor(Cog.getTag(msg.getAuthor()), null, msg.getAuthor().getEffectiveAvatarUrl())
                .setTitle("Error in command `" + cmd.name + '`')
                .setColor(Color.ORANGE)
                .appendDescription("```java\n")
                .appendDescription(stack.substring(0, Math.min(stack.length(), 2037)))
                .appendDescription("```")
                .addField("Timestamp", date.getTime() + "ms", true)
                .addField("Author ID", msg.getAuthor().getId(), true)
                .addField("Message ID", msg.getId(), true)
                .addField("Attachments", str(msg.getAttachments().size()), true)
                .addField("Guild", msg.getGuild() == null ? "None" : msg.getGuild().getName(), true)
                .addField("Guild ID", msg.getGuild() == null ? "None (no guild)" : msg.getGuild().getId(), true)
                .addField("Channel", msg.getChannel().getName(), true)
                .addField("Channel ID", msg.getChannel().getId(), true)
                .addField("Content", '`' + msg.getContent() + '`', true)
                .addField("Embeds", str(msg.getEmbeds().size()), true)
                .setFooter(date.toString(), null);

        if (msg.getGuild() != null)
            emb.setFooter(date.toString(), msg.getGuild().getIconUrl());

        return emb.build();
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

    public boolean isBot() {
        return jda.getSelfUser().isBot();
    }

    private long getUptimeMillis() {
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

    public static int start(String token, int shardCount, AccountType accountType, JSONObject config) throws LoginException, RateLimitedException {
        System.out.println("Starting...");

        if (shardCount < 1) {
            System.out.println("There needs to be at least 1 shard, or how will the bot work?");
            return 1;
        } else if (shardCount == 2) {
            System.out.println("2 shards is very buggy and doesn't work well. Use either 1 or 3+ shards.");
            return 1;
        }

        ShardUtil shardUtil = new ShardUtil(shardCount, config);

        IntStream.range(0, shardCount).forEach(shardId -> {
            Runnable monitor = () -> {
                final Logger logger = LogManager.getLogger("ShardMonitor " + shardId);
                while (true) {
                    Bot bot = new Bot();
                    JDABuilder builder = new JDABuilder(accountType)
                            .setToken(token)
                            .addEventListener(bot)
                            .setAudioEnabled(true)
                            .setAutoReconnect(true)
                            .setWebSocketTimeout(120000)
                            .setBulkDeleteSplittingEnabled(false)
                            .setStatus(OnlineStatus.ONLINE)
                            .setGame(Game.of("something"));

                    if ((System.getProperty("os.arch").startsWith("x86") ||
                            System.getProperty("os.arch").equals("amd64")) &&
                            (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX))
                        builder.setAudioSendFactory(new NativeAudioSendFactory());

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
