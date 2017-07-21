package com.khronodragon.bluestone.cogs;

import com.google.common.collect.ImmutableList;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.sql.Starboard;
import com.khronodragon.bluestone.sql.StarboardEntry;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.soap.Text;
import java.sql.SQLException;

public class StarboardCog extends Cog {
    private static final Logger logger = LogManager.getLogger(StarboardCog.class);
    private static final String NO_COMMAND = ":thinking: **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `create/new {channel name='starboard'}` - create a new starboard here (you may pass a different name)\n" +
            "    \u2022 `age [age] {units='days'}` - set the maximum age for messages to be starred\n" +
            "    \u2022 `lock` - lock the starboard\n" +
            "    \u2022 `unlock` - unlock the starboard\n" +
            "    \u2022 `threshold/min [number of stars]` - set the minimum number of stars required for a message to be starred\n" +
            "    \u2022 `clean [minimum number of stars]` - delete starboard entries without at least the specified amount of stars\n" +
            "    \u2022 `random` - show a random starred message\n" +
            "    \u2022 `show [message ID]` - show a starred message by its ID, or the ID in the starboard\n" +
            "    \u2022 `stats {@member}` - show starboard stats (optionally for a specific user)";
    private Dao<Starboard, Long> dao;
    private Dao<StarboardEntry, Integer> entryDao;

    public StarboardCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), Starboard.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard table!", e);
        }

        try {
            dao = DaoManager.createDao(bot.getShardUtil().getDatabase(), Starboard.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard DAO!", e);
        }

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), StarboardEntry.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard entry table!", e);
        }

        try {
            entryDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), StarboardEntry.class);
        } catch (SQLException e) {
            logger.warn("Failed to create starboard entry DAO!", e);
        }
    }

    public String getName() {
        return "Starboard";
    }

    public String getDescription() {
        return "A starboard! Upvote messages to get them on the starboard. It's like a leaderboard.";
    }

    @EventHandler
    public void onChannelDelete(TextChannelDeleteEvent event) throws SQLException {
        if (dao.idExists(event.getGuild().getIdLong())) {
            dao.deleteById(event.getGuild().getIdLong());

            entryDao.deleteBuilder()
                    .where()
                    .eq("guildId", event.getGuild().getIdLong())
                    .query();
        }
    }

    @EventHandler
    public void onReactionAdd(MessageReactionAddEvent event) throws SQLException {
        if (event.getGuild() == null || !dao.idExists(event.getGuild().getIdLong()))
            return;
    }

    @EventHandler
    public void onReactionRemove(MessageReactionRemoveEvent event) throws SQLException {
        logger.info("RR event");
    }

    @EventHandler
    public void onReactionRemove2(MessageReactionRemoveAllEvent event) throws SQLException {
        logger.info("RRA event");
    }

    private void requireStarboard(Context ctx) throws PassException, SQLException {
        if (!dao.idExists(ctx.guild.getIdLong())) {
            ctx.send("🚫 You must create a starboard first to use this command!").queue();
            throw new PassException();
        }
    }

    @Command(name = "star", desc = "Master starboard management command.", aliases = {"stars", "starboard", "starman"},
            thread = true, guildOnly = true)
    public void managementCommand(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        if (invoked.equals("create") || invoked.equals("new"))
            cmdCreate(ctx);
        else if (invoked.equals("age"))
            cmdAge(ctx);
        else if (invoked.equals("lock"))
            cmdLock(ctx);
        else if (invoked.equals("unlock"))
            cmdUnlock(ctx);
        else if (invoked.equals("limit") || invoked.equals("threshold") || invoked.equals("min"))
            cmdThreshold(ctx);
        else if (invoked.equals("clean"))
            cmdClean(ctx);
        else if (invoked.equals("random"))
            cmdRandom(ctx);
        else if (invoked.equals("show"))
            cmdShow(ctx);
        else if (invoked.equals("stats"))
            cmdStats(ctx);
        else
            ctx.send(NO_COMMAND).queue();
    }

    private void cmdCreate(Context ctx) throws SQLException {
        String channelName = "starboard";
        if (ctx.args.size() > 2) {
            String wantedName = ctx.args.get(1);
            if (wantedName.length() < 2 || wantedName.length() > 100) {
                ctx.send(Emotes.getFailure() + " Channel name must be between 2 and 100 characters long!").queue();
                return;
            }

            channelName = wantedName.toLowerCase();
        }

        TextChannel channel = (TextChannel) ctx.guild.getController().createTextChannel(channelName)
                .setTopic("Starboard created and managed by " + ctx.guild.getSelfMember().getAsMention())
                .addPermissionOverride(ctx.guild.getPublicRole(),
                        ImmutableList.of(Permission.MESSAGE_HISTORY),
                        ImmutableList.of(Permission.MESSAGE_WRITE))
                .addPermissionOverride(ctx.guild.getSelfMember(),
                        ImmutableList.of(Permission.MESSAGE_WRITE, Permission.MESSAGE_MANAGE,
                                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY,
                                Permission.MESSAGE_READ),
                        null).complete();

        Starboard starboard = new Starboard(channel.getIdLong(), ctx.guild.getIdLong(), 1, false);

        ctx.send("✨ Created " + channel.getAsMention() +
                " with a maximum message age of 1 week, and minimum star count of 1.").queue();
    }

    private void cmdAge(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        ctx.send("WIP").queue();
    }

    private void cmdLock(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        if (starboard.isLocked()) {
            ctx.send(Emotes.getFailure() + " Already locked!").queue();
        } else {
            starboard.setLocked(true);
            ctx.send(Emotes.getSuccess() + " Starboard locked.").queue();
        }
    }

    private void cmdUnlock(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        ctx.send("WIP").queue();
    }

    private void cmdThreshold(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        ctx.send("WIP").queue();
    }

    private void cmdClean(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        ctx.send("WIP").queue();
    }

    private void cmdRandom(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        ctx.send("WIP").queue();
    }

    private void cmdShow(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        ctx.send("WIP").queue();
    }

    private void cmdStats(Context ctx) throws SQLException {
        requireStarboard(ctx);
        Starboard starboard = dao.queryForId(ctx.guild.getIdLong());

        ctx.send("WIP").queue();
    }
}
