package com.khronodragon.bluestone;

import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDA.ShardInfo;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import javax.security.auth.login.LoginException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class Bot extends ListenerAdapter implements ClassUtilities {
    private ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(2);
    public ThreadPoolExecutor threadExecutor;
    private HashSet<ScheduledFuture> tasks = new HashSet<>();
    public HashMap<String, Command> commands = new HashMap<>();
    public HashMap<String, Cog> cogs = new HashMap<>();
    public HashMap<String, AtomicInteger> commandCalls = new HashMap<>();
    public ApplicationInfo appInfo;
    public User owner;
    private JDA jda;

    public Bot() {
        super();
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecHandlerImpl rejectionHandler = new RejectedExecHandlerImpl();
        threadExecutor = new ThreadPoolExecutor(3, 100, 25, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(1), threadFactory, rejectionHandler);
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    private static void sprint(String text) {
        System.out.println(text);
    }

    public int getShardNum(Event event) {
        ShardInfo sInfo = event.getJDA().getShardInfo();
        if (sInfo == null) {
            return 1;
        } else {
            return sInfo.getShardId() + 1;
        }
    }

    public int getShardTotal(Event event) {
        ShardInfo sInfo = event.getJDA().getShardInfo();
        if (sInfo == null) {
            return 1;
        } else {
            return sInfo.getShardTotal();
        }
    }

    public String vagueTrace(Throwable e) {
        StackTraceElement[] elements = e.getStackTrace();
        StackTraceElement[] limitedElems = {elements[0], elements[1]};
        List<String> stack = new ArrayList<>();
        stack.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        for (StackTraceElement elem: limitedElems) {
            String base = "> " + elem.getClassName() + "." + elem.getMethodName();
            base += elem.isNativeMethod() ? "(native)" : "()";
            stack.add(base);
        }
        return StringUtils.join(stack, "\n  ");
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
        printf("[Shard %d] Ready - ID: %d", getShardNum(event), uid);

        Runnable task = new Runnable() {
            public void run() {
                String statusLine;
                switch (ThreadLocalRandom.current().nextInt(1, 12)) {
                    case 1:
                        statusLine = String.format("with %s members", jda.getUsers().size());
                        break;
                    case 2:
                        statusLine = String.format("in %d channels", jda.getTextChannels().size() +
                                                   jda.getVoiceChannels().size());
                        break;
                    case 3:
                        statusLine = String.format("in %d servers", jda.getGuilds().size());
                        break;
                    case 4:
                        statusLine = String.format("in %d guilds", jda.getGuilds().size());
                        break;
                    case 5:
                        statusLine = String.format("from shard %d of %d", getShardNum(event), getShardTotal(event));
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
            }
        };

        ScheduledFuture future = scheduledExecutor.scheduleAtFixedRate(task, 10, 75, TimeUnit.SECONDS);
        tasks.add(future);

        Reflections reflector = new Reflections("com.khronodragon.bluestone.cogs");
        Set<Class<? extends Cog>> cogClasses = reflector.getSubTypesOf(Cog.class);
        for (Class cogClass: cogClasses) {
            try {
                Object obj = cogClass.getConstructor(this.getClass()).newInstance(this);
                ((Cog) obj).register();
            } catch (NoSuchMethodException e) {
                print("Failed to register cog" + cogClass.getName());
                e.printStackTrace();
            } catch (InstantiationException e) {
                print("Failed to register cog" + cogClass.getName());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                print("Failed to register cog" + cogClass.getName());
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                print("Failed to register cog" + cogClass.getName());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume(ResumedEvent event) {
        printf("[Shard %d] WebSocket resumed", getShardNum(event));
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        printf("[Shard %d] Reconnected", getShardNum(event));
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        printf("[Shard %d] Finished shutting down", getShardNum(event));
    }

    @Override
    public void onException(ExceptionEvent event) {
        printf("[Shard %d] Error: %s", getShardNum(event), event.getCause());
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
                    e.printStackTrace();
                    channel.sendMessage(":x: A severe internal error occurred.").queue();
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause == null) {
                        e.printStackTrace();
                        channel.sendMessage(":x: An unknown internal error occurred.").queue();
                    } else {
                        cause.printStackTrace();
                        channel.sendMessage(String.format(":warning: Error in `%s%s`:```java\n%s```", prefix, cmdName, vagueTrace(cause))).queue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

    public Message waitForMessage(float timeout, Predicate<Message> check) {
        ContainerCell<Message> lock = new ContainerCell<>();
        MessageWaitEventListener listener = new MessageWaitEventListener(lock, check);
        jda.addEventListener(listener);

        synchronized (lock) {
            if (timeout < 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    jda.removeEventListener(listener);
                    return null;
                }
            } else {
                long ltime = (long) timeout;
                try {
                    lock.wait(ltime, (int) (timeout - ltime) * (int) 1e9);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    jda.removeEventListener(listener);
                    return null;
                }
            }
            return lock.getValue();
        }
    }

    public static int start(String token, int shardCount, AccountType accountType) throws LoginException, RateLimitedException {
        sprint("Starting bot...");

        if (shardCount < 1) {
            sprint("There needs to be at least 1 shard, or how will the bot work?");
            return 1;
        }

        for (int shardId: IntStream.range(0, shardCount).toArray()) {
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

            if (shardCount > 1) {
                builder.useSharding(shardId, shardCount);
            }

            JDA jda = builder.buildAsync();
            bot.setJda(jda);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
        }

        return 0;
    }
}