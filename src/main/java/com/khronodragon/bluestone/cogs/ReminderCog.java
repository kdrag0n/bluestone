package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.sql.Reminder;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReminderCog extends Cog {
    private static final Logger logger = LogManager.getLogger(ReminderCog.class);
    private final Parser timeParser = new Parser();
    private Dao<Reminder, Integer> dao;

    public ReminderCog(Bot bot) {
        super(bot);

        dao = setupDao(Reminder.class);

        if (bot.getShardNum() == 1) {
            try {
                scheduleAllFromDB();
            } catch (SQLException e) {
                logger.warn("Failed to re-schedule all reminders from DB", e);
            }
        }
    }

    public String getName() {
        return "Reminder";
    }

    public String getDescription() {
        return "A cog that's all about reminding you of things.";
    }

    private void scheduleAllFromDB() throws SQLException {
        for (Reminder reminder: dao.queryForAll()) {
            schedule(reminder);
        }
    }

    private void schedule(Reminder reminder) {
        bot.getScheduledExecutor().schedule(() -> {
            try {
                dao.delete(reminder);
            } catch (SQLException ignored) {}

            bot.getJda().getUserById(reminder.getUserId()).openPrivateChannel().queue(channel ->
                    channel.sendMessage(new EmbedBuilder()
                            .setAuthor("Reminder", null, bot.getJda().getSelfUser().getEffectiveAvatarUrl())
                            .setDescription(reminder.getMessage())
                            .setFooter("You asked me to remind you of this at", null)
                            .setColor(randomColor())
                            .setTimestamp(Instant.now())
                            .build()).queue()
            );
        }, reminder.getRemindAt().getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Command(name = "remindme", desc = "Schedule a reminder for you at a certain time, over DM.",
            usage = "[time/date] [message", aliases = {"remind", "remind_me"})
    public void cmdRemindMe(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 2) {
            ctx.fail("I need a time/date (in any form), and message to remind you with!");
            return;
        }
        List<DateGroup> groups = timeParser.parse(ctx.rawArgs);

        String msg = "";
        Date date = null;
        for (DateGroup group: groups) {
            if (!group.getDates().isEmpty()) {
                date = group.getDates().get(0);
                msg = StringUtils.replaceOnce(ctx.rawArgs, group.getText(), "");
            }
        }

        if (msg.startsWith("to") && msg.length() > 2) {
            msg = msg.substring(2);
        }
        msg = msg.trim();

        if (date == null) {
            ctx.fail("Failed to parse time/date!");
            return;
        } else if (date.getTime() < System.currentTimeMillis()) {
            ctx.fail("That time is in the past!");
            return;
        } else if (date.getTime() > System.currentTimeMillis() + 157680000000L) {
            ctx.fail("That time is too far into the future!");
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
