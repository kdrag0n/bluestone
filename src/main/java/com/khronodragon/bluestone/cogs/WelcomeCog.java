package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.sql.GuildWelcomeMessages;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Date;
import java.util.regex.Pattern;

public class WelcomeCog extends Cog {
    private static final Logger logger = LogManager.getLogger(WelcomeCog.class);
    private static final String DEFAULT_WELCOME = "[mention] **Welcome to [server]!**\n" +
            "Enjoy your time here, and find out more about me with `[prefix]help`.";
    private static final String DEFAULT_LEAVE = "[rip] **[member_tag] has left the server...**";

    private static final Pattern SUB_REGEX = Pattern.compile("\\[([a-z_]+)]");
    private static final String NO_COMMAND = ":thinking: **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `status` - view the status of this message\n" +
            "    \u2022 `show` - show the current message\n" +
            "    \u2022 `set [message]` - set the current message\n" +
            "    \u2022 `toggle` - toggle the status of this message\n" +
            "    \u2022 `preview` - preview the current message\n" +
            "    \u2022 `channel [#channel]` - change the channel messages are sent in (default: default channel, or first available one)\n" +
            "    \u2022 `tags` - show the tags available for use in messages\n";
    private static final String TAG_HELP = "**Here are all the tags:**\n" +
            "    • `[mention/member_mention/member]` - @mention the member\n" +
            "    • `[member_name]` - the name of the member\n" +
            "    • `[member_tag]` - the tag (Username#XXXX) of the member\n" +
            "    • `[member_discrim]` - the member's discriminator\n" +
            "    • `[member_id]` - the member's user ID\n" +
            "    • `[server/server_name]` - the name of this server\n" +
            "    • `[server_icon]` - the link to this server's icon\n" +
            "    • `[server_id]` - this server's ID\n" +
            "    • `[server_owner]` - the name of this server's owner\n" +
            "    • `[time/date]` - the current date and time, like `Tue Jun 27 10:06:59 EDT 2017`\n" +
            "    • `[prefix]` - my command prefix in this server\n" +
            "    • `[bot_owner]` - the tag of my owner\n" +
            "    • `[rip]` - a gravestone\n" +
            "\n" +
            "Example message:```\n" +
            "[mention] Hey there, and welcome to [server]! The owner here is [server_owner]. You joined at [time]. " +
            "To use this bot, try [prefix]help. It was made by [bot_owner]. Have fun!```";
    private Dao<GuildWelcomeMessages, Long> messageDao;

    public WelcomeCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), GuildWelcomeMessages.class);
        } catch (SQLException e) {
            logger.warn("Failed to create welcome message table!", e);
        }

        try {
            messageDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), GuildWelcomeMessages.class);
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

    @Command(name = "welcome", desc = "Manage member welcome messages.", guildOnly = true,
            aliases = {"welcome_msgs", "welcomemsg"}, thread = true,
            perms = {"manageServer", "createInstantInvite", "manageRoles"}, usage = "[action] {args...}")
    public void welcomeControl(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        try {
            if (invoked.equals("status"))
                welcomeCmdStatus(ctx);
            else if (invoked.equals("show"))
                welcomeCmdShow(ctx);
            else if (invoked.equals("set"))
                welcomeCmdSet(ctx);
            else if (invoked.equals("toggle"))
                welcomeCmdToggle(ctx);
            else if (invoked.equals("preview"))
                welcomeCmdPreview(ctx);
            else if (invoked.equals("channel"))
                controlCmdChannel(ctx);
            else if (invoked.equals("tags"))
                allCmdHelp(ctx);
            else
                ctx.send(NO_COMMAND).queue();
        } catch (NullPointerException e) {
            ctx.send(Emotes.getFailure() + " Something's not right with this server's message settings. Let me try to fix that...").queue();

            try {
                messageDao.createOrUpdate(new GuildWelcomeMessages(ctx.guild.getIdLong(),
                        "[default]", "[default]", true, true));
            } catch (SQLException ex) {
                logger.error("Failed to recover from NPE (control cmd)", ex);
                ctx.send(":sob: I wasn't able to fix it for you. If it's a problem, contact the bot owner.").queue();
                return;
            }

            ctx.send(Emotes.getSuccess() + " I fixed it for you! Try your command again.").queue();
        }
    }

    private void welcomeCmdStatus(Context ctx) throws SQLException {
        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());
        String st = query.isWelcomeEnabled() ? "on" : "off";

        ctx.send("The welcome message is currently **" + st + "**.").queue();
    }

    private void welcomeCmdShow(Context ctx) throws SQLException {
        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());

        ctx.send("**Here's the current welcome message:**\n\n```\n" + query.getWelcome() + "```").queue();
    }

    private void welcomeCmdSet(Context ctx) throws SQLException {
        if (ctx.args.size() < 2) {
            ctx.send(Emotes.getFailure() + " I need a new message to set!").queue();
            return;
        }
        String newMessage = ctx.rawArgs.substring(3).trim();

        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());
        query.setWelcome(newMessage);

        messageDao.update(query);
        ctx.send(Emotes.getSuccess() + " Welcome message set.").queue();
    }

    private void welcomeCmdToggle(Context ctx) throws SQLException {
        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());
        query.setWelcomeEnabled(!query.isWelcomeEnabled());
        String st = query.isWelcomeEnabled() ? "on" : "off";

        messageDao.update(query);
        ctx.send(Emotes.getSuccess() + " The welcome message is now **" + st + "**.").queue();
    }

    private void welcomeCmdPreview(Context ctx) {
        onGuildMemberJoin(new GuildMemberJoinEvent(ctx.jda, ctx.event.getResponseNumber(),
                ctx.guild, ctx.member));
    }

    private void controlCmdChannel(Context ctx) throws SQLException {
        if (ctx.message.getMentionedChannels().size() < 1) {
            ctx.send(Emotes.getFailure() + " I need a channel to use!").queue();
            return;
        }
        TextChannel channel = ctx.message.getMentionedChannels().get(0);

        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());
        query.setChannelId(channel.getIdLong());
        messageDao.update(query);

        ctx.send(Emotes.getSuccess() + " Welcome and leave channel changed to " + channel.getAsMention() + '.').queue();
    }

    @Command(name = "leave", desc = "Manage member leave messages.", guildOnly = true,
            aliases = {"leave_msgs", "leavemsg"}, thread = true,
            perms = {"manageServer", "createInstantInvite", "manageRoles"}, usage = "[action] {args...}")
    public void leaveControl(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        try {
            if (invoked.equals("status"))
                leaveCmdStatus(ctx);
            else if (invoked.equals("show"))
                leaveCmdShow(ctx);
            else if (invoked.equals("set"))
                leaveCmdSet(ctx);
            else if (invoked.equals("toggle"))
                leaveCmdToggle(ctx);
            else if (invoked.equals("preview"))
                leaveCmdPreview(ctx);
            else if (invoked.equals("channel"))
                controlCmdChannel(ctx);
            else if (invoked.equals("tags"))
                allCmdHelp(ctx);
            else
                ctx.send(NO_COMMAND).queue();
        } catch (NullPointerException e) {
            logger.warn("Message control: NPE", e);
            ctx.send(Emotes.getFailure() + " Something's not right with this server's message settings. Let me try to fix that...").queue();

            try {
                messageDao.createOrUpdate(new GuildWelcomeMessages(ctx.guild.getIdLong(),
                        "[default]", "[default]", true, true));
            } catch (SQLException ex) {
                logger.error("Failed to recover from NPE (control cmd)", ex);
                ctx.send(":robot: I wasn't able to fix it for you. If it's a problem, contact the bot owner.").queue();
                return;
            }

            ctx.send(Emotes.getSuccess() + " I fixed it for you! Try your command again.").queue();
        }
    }

    private void leaveCmdStatus(Context ctx) throws SQLException {
        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());
        String st = query.isLeaveEnabled() ? "on" : "off";

        ctx.send("The leave message is currently **" + st + "**.").queue();
    }

    private void leaveCmdShow(Context ctx) throws SQLException {
        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());

        ctx.send("**Here's the current leave message:**\n\n```\n" + query.getLeave() + "```").queue();
    }

    private void leaveCmdSet(Context ctx) throws SQLException {
        if (ctx.args.size() < 2) {
            ctx.send(Emotes.getFailure() + " I need a new message to set!").queue();
            return;
        }
        String newMessage = ctx.rawArgs.substring(3).trim();

        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());
        query.setLeave(newMessage);

        messageDao.update(query);
        ctx.send(Emotes.getSuccess() + " Leave message set.").queue();
    }

    private void leaveCmdToggle(Context ctx) throws SQLException {
        GuildWelcomeMessages query = messageDao.queryForId(ctx.guild.getIdLong());
        query.setLeaveEnabled(!query.isLeaveEnabled());
        String st = query.isLeaveEnabled() ? "on" : "off";

        messageDao.update(query);
        ctx.send(Emotes.getSuccess() + " The leave message is now **" + st + "**.").queue();
    }

    private void leaveCmdPreview(Context ctx) {
        onGuildMemberLeave(new GuildMemberLeaveEvent(ctx.jda, ctx.event.getResponseNumber(),
                ctx.guild, ctx.member));
    }

    private GuildWelcomeMessages initGuild(Guild guild) throws SQLException {
        GuildWelcomeMessages obj = new GuildWelcomeMessages(guild.getIdLong(),
                "[default]", "[default]", true, true);
        messageDao.createOrUpdate(obj);

        return obj;
    }

    private String formatMessage(String msg, Guild guild, Member member, String def) {
        return Strings.replace(msg.replace("[default]", def), SUB_REGEX, m -> {
            return Strings.createMap()
                    .map("mention", member::getAsMention)
                    .map("member_name", member::getEffectiveName)
                    .map("member_tag", () -> getTag(member.getUser()))
                    .map("member_discrim", member.getUser()::getDiscriminator)
                    .map("member_id", member.getUser()::getId)
                    .map("server", guild::getName)
                    .map("server_icon", guild::getIconUrl)
                    .map("server_id", guild::getId)
                    .map("server_owner", guild.getOwner()::getEffectiveName)
                    .map("member", member::getAsMention)
                    .map("member_mention", member::getAsMention)
                    .map("time", () -> new Date().toString())
                    .map("date", () -> new Date().toString())
                    .map("server_name", guild::getName)
                    .map("prefix", () -> bot.getShardUtil().getPrefixStore().getPrefix(guild.getIdLong()))
                    .map("bot_owner", "Dragon5232#1841")
                    .map("rip", Emotes::getGrave)
                    .exec(m);
        });
    }

    private void allCmdHelp(Context ctx) {
        ctx.send(TAG_HELP).queue();
    }

    @EventHandler(threaded = true)
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getMember().getUser().getIdLong() == bot.getJda().getSelfUser().getIdLong())
            return;

        if (!event.getGuild().isAvailable())
            return;

        try {
            GuildWelcomeMessages queryResult = messageDao.queryForId(event.getGuild().getIdLong());

            if (queryResult == null) {
                try {
                    queryResult = initGuild(event.getGuild());
                } catch (SQLException ex) {
                    logger.error("Failed to init message object (join event)", ex);
                    return;
                }
            }
            if (!queryResult.isWelcomeEnabled()) return;

            TextChannel channel;
            if (queryResult.getChannelId() == 0L) {
                channel = event.getGuild().getPublicChannel();

                if (channel == null) {
                    for (TextChannel potential: event.getGuild().getTextChannels()) {
                        if (potential.canTalk())
                            channel = potential;
                    }
                }

                if (channel == null)
                    return;
            } else {
                channel = event.getGuild().getTextChannelById(queryResult.getChannelId());
            }

            if (!channel.canTalk())
                return;

            String msg = formatMessage(queryResult.getWelcome(), event.getGuild(),
                    event.getMember(), DEFAULT_WELCOME);

            channel.sendMessage(Context.truncate(msg)).queue();
        } catch (SQLException e) {
            logger.error("SQL error while handling member join", e);
        }
    }

    @EventHandler(threaded = true)
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getMember().getUser().getIdLong() == bot.getJda().getSelfUser().getIdLong())
            return;

        if (!event.getGuild().isAvailable())
            return;

        try {
            GuildWelcomeMessages queryResult = messageDao.queryForId(event.getGuild().getIdLong());

            if (queryResult == null) {
                try {
                    queryResult = initGuild(event.getGuild());
                } catch (SQLException ex) {
                    logger.error("Failed to init message object (leave event)", ex);
                    return;
                }
            }
            if (!queryResult.isLeaveEnabled()) return;

            TextChannel channel;
            if (queryResult.getChannelId() == 0L) {
                channel = event.getGuild().getPublicChannel();

                if (channel == null) {
                    for (TextChannel potential: event.getGuild().getTextChannels()) {
                        if (potential.canTalk())
                            channel = potential;
                    }
                }

                if (channel == null)
                    return;
            } else {
                channel = event.getGuild().getTextChannelById(queryResult.getChannelId());
            }

            if (!channel.canTalk())
                return;

            String msg = formatMessage(queryResult.getLeave(), event.getGuild(),
                    event.getMember(), DEFAULT_LEAVE);

            channel.sendMessage(Context.truncate(msg)).queue();
        } catch (SQLException e) {
            logger.error("SQL error while handling member leave", e);
        }
    }

    @EventHandler(threaded = true)
    public void onGuildJoin(GuildJoinEvent event) {
        if (!event.getGuild().isAvailable()) return;

        try {
            messageDao.createOrUpdate(new GuildWelcomeMessages(event.getGuild().getIdLong(),
                    "[default]", "[default]", true, true));
        } catch (SQLException e) {
            logger.warn("Failed to create WelcomeMessages for guild {}", event.getGuild().getId(), e);
        }
    }

    @EventHandler(threaded = true)
    public void onGuildLeave(GuildLeaveEvent event) {
        if (!event.getGuild().isAvailable()) return;

        try {
            messageDao.deleteById(event.getGuild().getIdLong());
        } catch (SQLException e) {
            logger.warn("Failed to delete WelcomeMessages of guild {}", event.getGuild().getId(), e);
        }
    }
}
