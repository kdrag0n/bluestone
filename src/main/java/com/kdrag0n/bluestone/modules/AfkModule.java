package com.kdrag0n.bluestone.modules;

import com.censhare.db.iindex.IDSetTrie;
import com.j256.ormlite.dao.Dao;
import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.Module;
import com.kdrag0n.bluestone.Context;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.annotations.EventHandler;
import com.kdrag0n.bluestone.sql.AfkMessage;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.List;

public class AfkModule extends Module {
    private static final Logger logger = LoggerFactory.getLogger(AfkModule.class);
    private static final IDSetTrie afkUsers = new IDSetTrie(48);
    private final Dao<AfkMessage, Long> dao;

    public AfkModule(Bot bot) {
        super(bot);

        dao = setupDao(AfkMessage.class);

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
    @EventHandler()
    public void onMsg(GuildMessageReceivedEvent event) throws SQLException {
        if (event.getAuthor().isBot())
            return;

        if (afkUsers.get(event.getAuthor().getIdLong())) {
            afkUsers.clear(event.getAuthor().getIdLong());
            dao.deleteById(event.getAuthor().getIdLong());
        }

        if (event.getMessage().getMentionedUsers().size() != 0) {
            List<User> mentioned = event.getMessage().getMentionedUsers();
            StringBuilder message = new StringBuilder(event.getAuthor().getAsMention()).append(' ');
            TLongSet added = new TLongHashSet();

            for (int i = 0; i < mentioned.size(); ++i) {
                User user = mentioned.get(i);
                if (user.isBot())
                    continue;
                if (!afkUsers.get(user.getIdLong()))
                    continue;

                AfkMessage afkMessage = dao.queryForId(user.getIdLong());
                if (afkMessage != null && !added.contains(user.getIdLong())) {
                    message.append("**").append(event.getGuild().getMember(user).getEffectiveName())
                            .append("** isn't available at the moment. \"").append(afkMessage.getMessage())
                            .append("\"\n");

                    added.add(user.getIdLong());
                    if (added.size() >= 5 && i + 1 < mentioned.size()) {
                        message.append("... and **").append(mentioned.size() - (i + 1))
                                .append("** more unavailable people");
                    }
                }
            }

            if (added.size() != 0) {
                event.getChannel().sendMessage(Context.filterMessage(message.toString())).queue();
            }
        }
    }

    @Command(name = "afk", desc = "Set an AFK message, indicating that you're currently away. Automatically removes message when you're back.", usage = "[message]", aliases = {
            "away" })
    public void cmdAfk(Context ctx) throws SQLException {
        AfkMessage afkMessage = dao.queryForId(ctx.author.getIdLong());

        if (ctx.args.empty && afkMessage == null) {
            ctx.fail(" You must specify why you're going away!");
            return;
        } else if (ctx.rawArgs.length() > 150) {
            ctx.fail("Your AFK message can't be longer than 150 characters!");
            return;
        } else if (afkMessage != null) {
            ctx.success("You're no longer away.");
            return;
        }

        afkUsers.set(ctx.author.getIdLong());
        afkMessage = new AfkMessage(ctx.author.getIdLong(), ctx.rawArgs);
        dao.createOrUpdate(afkMessage);

        ctx.success("You're now away.");
    }
}
