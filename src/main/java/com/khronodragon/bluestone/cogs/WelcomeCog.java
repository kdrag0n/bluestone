package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.EventedCog;
import com.khronodragon.bluestone.sql.GuildWelcomeMessage;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

public class WelcomeCog extends Cog implements EventedCog {
    private static final Logger logger = LogManager.getLogger(WelcomeCog.class);
    private Dao<GuildWelcomeMessage, Long> messageDao;

    public WelcomeCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), GuildWelcomeMessage.class);
        } catch (SQLException e) {
            logger.warn("Failed to create welcome message table!", e);
        }

        try {
            messageDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), GuildWelcomeMessage.class);
        } catch (SQLException e) {
            logger.warn("Failed to create welcome message DAO!", e);
        }
    }

    public String getName() {
        return "Welcome";
    }

    public String getDescription() {
        return "The cog that welcomes people.";
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getMember().getUser().getIdLong() == bot.getJda().getSelfUser().getIdLong())
            return;

//        if (event.getGuild().getIdLong())
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getMember().getUser().getIdLong() == bot.getJda().getSelfUser().getIdLong())
            return;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {

    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        try {
            if (messageDao.queryForId(event.getGuild().getIdLong()) != null) {

            }
        } catch (SQLException e) {
            logger.warn("Failed to query for ID of guild I just left...", e);
        }
    }
}
