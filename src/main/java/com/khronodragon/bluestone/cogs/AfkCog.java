package com.khronodragon.bluestone.cogs;

import com.censhare.db.iindex.IDMapTrie;
import com.censhare.db.iindex.IDSetTrie;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.sql.AfkMessage;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

public class AfkCog extends Cog {
    private static final Logger logger = LogManager.getLogger(AfkCog.class);
    private static final IDSetTrie afkUsers = new IDSetTrie(48);
    private Dao<AfkMessage, Long> dao;

    public AfkCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), AfkMessage.class);
        } catch (SQLException e) {
            logger.error("Failed to create AFK message table!", e);
        }

        try {
            dao = DaoManager.createDao(bot.getShardUtil().getDatabase(), AfkMessage.class);
        } catch (SQLException e) {
            logger.error("Failed to create AFK message DAO!", e);
        }

        try {
            for (AfkMessage afkMessage : dao.queryForAll()) {
                afkUsers.set(afkMessage.getUserId());
            }
        } catch (SQLException e) {
            logger.error("Failed to get AFK user list!", e);
        }
    }

    public String getName() {
        return "AFK";
    }

    public String getDescription() {
        return "Away-from-keyboard messages.";
    }

    @EventHandler(threaded = true)
    public void onMsg(GuildMessageReceivedEvent event) throws SQLException {
        if (event.getAuthor().isBot()) return;

        if (afkUsers.get(event.getAuthor().getIdLong())) {
            afkUsers.clear(event.getAuthor().getIdLong());
            dao.deleteById(event.getAuthor().getIdLong());
        }

        if (event.getMessage().getMentionedUsers().size() != 0) {
            List<User> mentioned = event.getMessage().getMentionedUsers();
            StringBuilder message = new StringBuilder(event.getAuthor().getAsMention())
                    .append(' ');
            TLongSet added = new TLongHashSet();

            for (int i = 0; i < mentioned.size(); ++i) {
                User user = mentioned.get(i);
                if (user.isBot()) continue;
                if (!afkUsers.get(user.getIdLong())) continue;

                AfkMessage afkMessage = dao.queryForId(user.getIdLong());
                if (afkMessage != null && !added.contains(user.getIdLong())) {
                    message.append("**")
                            .append(event.getGuild().getMember(user).getEffectiveName())
                            .append("** isn't available at the moment. \"")
                            .append(afkMessage.getMessage())
                            .append("\"\n");

                    added.add(user.getIdLong());
                    if (added.size() >= 5 && i + 1 < mentioned.size()) {
                        message.append("... and **")
                                .append(mentioned.size() - (i + 1))
                                .append("** more unavailable people");
                    }
                }
            }

            if (added.size() != 0) {
                event.getChannel().sendMessage(Context.filterMessage(message.toString())).queue();
            }
        }
    }

    @Command(name = "afk",
            desc = "Set an AFK message, indicating that you're currently away. Automatically removes message when you're back.",
            usage = "[message]", thread = true, aliases = {"away"})
    public void cmdAfk(Context ctx) throws SQLException {
        AfkMessage afkMessage = dao.queryForId(ctx.author.getIdLong());

        if (ctx.rawArgs.length() < 1 && afkMessage == null) {
            ctx.send(Emotes.getFailure() + "  You must specify why you're going away!").queue();
            return;
        } else if (ctx.rawArgs.length() > 150) {
            ctx.send(Emotes.getFailure() + " Your AFK message can't be longer than 150 characters!").queue();
            return;
        } else if (afkMessage != null) {
            ctx.send(Emotes.getSuccess() + " You're no longer away.").queue();
            return;
        }

        afkUsers.set(ctx.author.getIdLong());
        afkMessage = new AfkMessage(ctx.author.getIdLong(), ctx.rawArgs);
        dao.createOrUpdate(afkMessage);

        ctx.send(Emotes.getSuccess() + " You're now away.").queue();
    }
}
