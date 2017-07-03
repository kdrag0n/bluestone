package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.annotations.EventHandler;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm a");
    private static final long monitorGuildId = 250780048943087618L;
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

    @EventHandler
    public void onGuildJoin(GuildJoinEvent event) {
        TextChannel channel = findChannel(guildEventChannelId);

        if (!event.getGuild().isAvailable()) {
            channel.sendMessage("<:plus:331224997362139136> __**Guild Unavailable**__ | ? members, ? emotes | `" +
                    event.getGuild().getIdLong() + "` | Current Total: " + bot.getShardUtil().getGuildCount()).queue();
            return;
        }

        channel.sendMessage("<:plus:331224997362139136> __**" + event.getGuild().getName() + "**__ - " +
                event.getGuild().getMembers().size() + " members, " + event.getGuild().getEmotes().size() +
                " emotes | `" + event.getGuild().getId() + "` | Current Total: " +
                bot.getShardUtil().getGuildCount()).queue();
    }

    @EventHandler
    public void onGuildLeave(GuildLeaveEvent event) {
        TextChannel channel = findChannel(guildEventChannelId);

        if (!event.getGuild().isAvailable()) {
            channel.sendMessage("<:minus:331225043445088258> __**Guild Unavailable**__ | ? members, ? emotes | `" +
                    event.getGuild().getIdLong() + "` | Current Total: " + bot.getShardUtil().getGuildCount()).queue();
            return;
        }

        channel.sendMessage("<:minus:331225043445088258> __**" + event.getGuild().getName() + "**__ - " +
                event.getGuild().getMembers().size() + " members, " + event.getGuild().getEmotes().size() +
                " emotes | `" + event.getGuild().getId() + "` | Current Total: " +
                bot.getShardUtil().getGuildCount()).queue();
    }

    private TextChannel findChannel(long channelId) {
        int shardId = (int) ((monitorGuildId >> 22) % bot.getShardTotal());

        return bot.getShardUtil().getShard(shardId).getJda().getTextChannelById(channelId);
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

            String message = e.getMessage().getFormattedMessage();
            if (!isValid(message)) return;

            if (e.getThrown() != null) {
                message += '\n' + Bot.renderStackTrace(e.getThrown(), "    ", "at ");
            }

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

            message = format(":clock{0}: {1} <:spool:331187771173634049>{3} {2} **{5}**: {4}",
                    clockNum.toString(), LocalDateTime.ofInstant(Instant.ofEpochMilli(e.getTimeMillis()),
                            ZoneId.of("US/Pacific")).format(TIME_FORMAT),
                    levelEmote, e.getThreadName(), message, e.getLoggerName());

            for (String line : StringUtils.split(message, '\n')) {
                if (StringUtils.isBlank(line))
                    continue;

                String processedLine = line;
                if (processedLine.startsWith(" "))
                    processedLine = '\u200b' + processedLine;

                messageQueue.add(new MessageBuilder()
                        .append(processedLine.substring(0, Math.min(processedLine.length(), 2000)))
                        .build());
            }
        }

        private boolean isValid(String input) {
            return input != null &&
                    !input.replace(" ", "").replace("\n", "").isEmpty();
        }
    }
}