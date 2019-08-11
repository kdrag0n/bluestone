package com.kdrag0n.bluestone.modules;

import com.j256.ormlite.dao.Dao;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.Context;
import com.kdrag0n.bluestone.Emotes;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.sql.Reminder;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReminderModule extends Module {
    private static final Logger logger = LoggerFactory.getLogger(ReminderModule.class);
    private final Parser timeParser = new Parser();
    private final Dao<Reminder, Integer> dao;

    public ReminderModule(Bot bot) {
        super(bot);

        dao = setupDao(Reminder.class);

        try {
            scheduleAllFromDB();
        } catch (SQLException e) {
            logger.warn("Failed to re-schedule all reminders from DB", e);
        }
    }

    public String getName() {
        return "Reminder";
    }

    private void scheduleAllFromDB() throws SQLException {
        for (Reminder reminder : dao.queryForAll()) {
            schedule(reminder);
        }
    }

    private void schedule(Reminder reminder) {
        Bot.scheduledExecutor.schedule(() -> {
            try {
                dao.delete(reminder);
            } catch (SQLException ignored) {
            }

            bot.manager.getUserById(reminder.getUserId())
                    .openPrivateChannel()
                    .queue(channel -> channel.sendMessage(new EmbedBuilder()
                            .setAuthor("Reminder", null, bot.selfUser.getEffectiveAvatarUrl())
                            .setDescription(reminder.getMessage())
                            .setFooter("You asked me to remind you of this at", null).setColor(randomColor())
                            .setTimestamp(Instant.now())
                            .build()).queue());
        }, reminder.getRemindAt().getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Command(name = "remindme", desc = "Schedule a reminder for you at a certain time, over DM.", usage = "[time/date] [message", aliases = {
            "remind", "remind_me" })
    public void cmdRemindMe(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 2) {
            ctx.fail("I need a time/date (in any form), and message to remind you with!");
            return;
        }
        List<DateGroup> groups = timeParser.parse(ctx.rawArgs);

        String msg = "";
        Date date = null;
        for (DateGroup group : groups) {
            if (!group.getDates().isEmpty()) {
                date = group.getDates().get(0);
                msg = StringUtils.replaceOnce(ctx.rawArgs, group.getText(), "");
            }
        }

        if (msg.startsWith("to") && msg.length() > 2) {
            msg = msg.substring(2);
        }
        msg = msg.trim();

        if (date == null || date.getTime() < System.currentTimeMillis()) {
            ctx.fail("Invalid time/date!");
            return;
        } else if (date.getTime() > System.currentTimeMillis() + 63072000000L) { // 2 years
            ctx.fail("That time/date is too far in the future!");
            return;
        } else if (msg.length() < 1) {
            ctx.fail("I can't remind you of nothing!");
            return;
        }

        Reminder reminder = new Reminder(ctx.author.getIdLong(), msg, date);
        dao.createOrUpdate(reminder);
        schedule(reminder);

        ctx.send(Emotes.getSuccess() + " I will remind you at " + date + '.').queue();
    }
}
