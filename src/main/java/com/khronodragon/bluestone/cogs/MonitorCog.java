package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.annotations.EventHandler;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.LinkedList;
import java.util.Queue;

import static java.text.MessageFormat.format;

public class MonitorCog extends Cog {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(MonitorCog.class);
    private static final long consoleChannelId = 331144992003325955L;
    private static final long guildEventChannelId = 331145024161054720L;
    private LogMessageQueueSender queueSender;
    private Queue<Message> messageQueue;

    public MonitorCog(Bot bot) {
        super(bot);

        if (bot.getJda().getTextChannelById(consoleChannelId) != null) {
            queueSender = new LogMessageQueueSender();
            messageQueue = new LinkedList<>();

            Logger rootLogger = (Logger) LogManager.getRootLogger();
            if (rootLogger.getAppenders().containsKey("Bot-LogBridge"))
                rootLogger.removeAppender(rootLogger.getAppenders().get("Bot-LogBridge"));
            rootLogger.addAppender(new ConsoleAppender());

            queueSender.start();
        }
    }

    public String getName() {
        return "MonitorCog";
    }

    public String getDescription() {
        return "A description.";
    }

    public void unload() {
        super.unload();
        queueSender.interrupt();
    }

    @EventHandler(event = GuildJoinEvent.class)
    public void onGuildJoin(GuildJoinEvent event) {
        TextChannel channel = event.getJDA().getTextChannelById(guildEventChannelId);
        channel.sendMessage("<:plus:331224997362139136> __**" + event.getGuild().getName() + "**__ - " +
                            event.getGuild().getMembers().size() + " members, " + event.getGuild().getEmotes().size() +
                            " emotes | `" + event.getGuild().getId() + "` | Current Total: " +
                            bot.getShardUtil().getGuildCount()).queue();
    }

    @EventHandler(event = GuildLeaveEvent.class)
    public void onGuildLeave(GuildLeaveEvent event) {
        TextChannel channel = event.getJDA().getTextChannelById(guildEventChannelId);
        channel.sendMessage("<:minus:331225043445088258> __**" + event.getGuild().getName() + "**__ - " +
                event.getGuild().getMembers().size() + " members, " + event.getGuild().getEmotes().size() +
                " emotes | `" + event.getGuild().getId() + "` | Current Total: " +
                bot.getShardUtil().getGuildCount()).queue();
    }

    private TextChannel getConsoleChannel() {
        return bot.getJda().getTextChannelById(consoleChannelId);
    }

    private class LogMessageQueueSender extends Thread {
        private Field embedField = null;

        private LogMessageQueueSender() {
            super("Discord Log Sender");

            try {
                embedField = MessageBuilder.class.getDeclaredField("embed");
            } catch (NoSuchFieldException e) {
                logger.error("MessageBuilder missing 'embed' field", e);
                return;
            }
            embedField.setAccessible(true);
        }

        private boolean hasEmbed(MessageBuilder builder) {
            try {
                return embedField.get(builder) != null;
            } catch (IllegalAccessException ignored) {
                return false; // should never happen anyways
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    TextChannel channel = getConsoleChannel();
                    if (channel == null) continue;

                    MessageBuilder builder = new MessageBuilder();
                    Message item = messageQueue.poll();

                    while (item != null) {
                        if (builder.length() + item.getRawContent().length() + 1 > 2000 ||
                                (item.getEmbeds().size() > 0 && hasEmbed(builder))) {
                            channel.sendMessage(builder.build()).queue();
                            builder = new MessageBuilder();
                        }

                        builder.append(item.getRawContent())
                                .append('\n');

                        if (item.getEmbeds().size() > 0)
                            builder.setEmbed(item.getEmbeds().get(0));

                        item = messageQueue.poll();
                    }

                    if (!builder.isEmpty()) {
                        Message message = builder.build();

                        if (StringUtils.isNotBlank(message.getRawContent()))
                            channel.sendMessage(message).queue();
                    }

                    Thread.sleep(2500);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    @Plugin(name = "Bot-LogBridge", category = "Core", elementType = "appender", printObject = true)
    private class ConsoleAppender extends AbstractAppender {
        private ConsoleAppender() {
            super("Bot-LogBridge", null, PatternLayout.newBuilder()
                    .withPattern("[%d{HH:mm:ss} %level]: %msg")
                    .withAlwaysWriteExceptions(true)
                    .withNoConsoleNoAnsi(true)
                    .build(), false);
        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public void append(LogEvent e) {
            TextChannel channel = bot.getJda().getTextChannelById(consoleChannelId);
            if (channel == null || !channel.canTalk())
                return;

            String line = e.getMessage().getFormattedMessage();
            if (!isValid(line)) return;

            StringBuilder clockNum = new StringBuilder();
            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.getTimeMillis()),
                    ZoneId.systemDefault());
            int hour = time.get(ChronoField.HOUR_OF_AMPM) + 1;
            int mins = time.get(ChronoField.MINUTE_OF_HOUR) + 1;

            clockNum.append(hour);
            if (mins >= 30)
                clockNum.append("30");

            String levelEmote;
            Level level = e.getLevel();
            if (level == Level.TRACE)
                levelEmote = "👣";
            else if (level == Level.DEBUG)
                levelEmote = "🐛";
            else if (level == Level.INFO)
                levelEmote = "ℹ";
            else if (level == Level.WARN)
                levelEmote = "⚠";
            else if (level == Level.ERROR)
                levelEmote = "❌";
            else if (level == Level.FATAL)
                levelEmote = "🚨";
            else
                levelEmote = "🤷";

            line = format(":clock{0}: {1} [<:spool:331187771173634049>{3}] {2} {5}: {4}",
                    clockNum.toString(), LocalDateTime.ofInstant(Instant.ofEpochMilli(e.getTimeMillis()),
                            ZoneId.of("US/Pacific")).format(DateTimeFormatter.ofPattern("EEE MM dd hh:mm aa")),
                    levelEmote, e.getThreadName(), line, e.getLoggerName());

            messageQueue.add(new MessageBuilder().append(line).build());
        }

        private boolean isValid(String input) {
            return input != null &&
                    !input.replace(" ", "").replace("\n", "").isEmpty();
        }
    }

    public static void setupJdaLogging() {
        SimpleLog.LEVEL = SimpleLog.Level.OFF;

        SimpleLog.addListener(new SimpleLog.LogListener() {
            @Override
            public void onLog(SimpleLog log, SimpleLog.Level level, Object msg) {
                org.apache.logging.log4j.Logger logger = LogManager.getLogger(log.name);

                switch (level) {
                    case TRACE:
                        logger.trace(msg);
                        break;
                    case DEBUG:
                        logger.debug(msg);
                        break;
                    case FATAL:
                        logger.fatal(msg);
                        break;
                    case WARNING:
                        logger.warn(msg);
                        break;
                    case INFO:
                        logger.info(msg);
                        break;
                    default:
                        logger.info("[unknown] {}", msg);
                        break;
                }
            }

            @Override
            public void onError(SimpleLog log, Throwable err) {
                org.apache.logging.log4j.Logger logger = LogManager.getLogger(log.name);

                logger.warn("JDA errored", err);
            }
        });
    }
}
