package com.kdrag0n.bluestone.cogs;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.j256.ormlite.dao.Dao;
import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.annotations.DoNotAutoload;
import com.kdrag0n.bluestone.annotations.EventHandler;
import com.kdrag0n.bluestone.sql.GuildMemberActions;
import com.kdrag0n.bluestone.sql.GuildRoleOption;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DoNotAutoload
public class RolemanCog extends Cog {
    private static final Logger logger = LogManager.getLogger(RolemanCog.class);
    private static final String NO_COMMAND = "🤔 **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `[id]` - show quote `id`\n" +
            "    \u2022 `add [quote]` - add a quote\n" +
            "    \u2022 `delete [id]` - delete a quote, if you own it\n" +
            "    \u2022 `list` - list all the quotes (paginated)\n" +
            "    \u2022 `random` - view a random quote\n" +
            "    \u2022 `count` - see how many quotes there are\n" +
            "    \u2022 `message [message id]` - quote a message by its ID\n" +
            "\n" +
            "**Note**: this is ";
    private static final Object TRUE = new Object();
    private static final Object FALSE = new Object();
    private final LoadingCache<Long, Object> guildIsUsing = CacheBuilder.newBuilder()
            .concurrencyLevel(6)
            .maximumSize(750)
            .initialCapacity(25)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, Object>() {
                @Override
                public Object load(@Nonnull Long key) throws SQLException {
                    return dao.queryBuilder()
                            .where()
                            .eq("guildId", key)
                            .query()
                            .size() > 0 ? TRUE : FALSE;
                }
            });
    private final Dao<GuildRoleOption, Long> dao;
    private final Dao<GuildMemberActions, Integer> profDao;

    public RolemanCog(Bot bot) {
        super(bot);

        dao = setupDao(GuildRoleOption.class);
        profDao = setupDao(GuildMemberActions.class);
    }

    public String getName() {
        return "Role Manager";
    }

    public String getDescription() {
        return "The role manager that allows users to get roles.";
    }

    @EventHandler(threaded = true)
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) throws SQLException {
        long guildId = event.getGuild().getIdLong();

        if (guildIsUsing.getUnchecked(guildId) == TRUE) {
            long userId = event.getAuthor().getIdLong();

            GuildMemberActions prof = profDao.queryBuilder()
                    .where()
                    .eq("userId", userId)
                    .eq("guildId", guildId)
                    .queryForFirst();
            if (prof == null)
                prof = new GuildMemberActions(userId, guildId, (short)0, false, false);

            List<User> mU = event.getMessage().getMentionedUsers();
            if (mU.size() > 0 && mU.get(0) != event.getAuthor()) {
                prof.hasMentionedOther = true;
            }

            if (prof.messagesSent < 100)
                prof.messagesSent++;

            profDao.createOrUpdate(prof);
        }
    }

    @Command(name = "role", desc = "Get or remove a role.", aliases = {"rank"}, usage = "[role name]",
            thread = true, guildOnly = true)
    public void cmdRole(Context ctx) throws SQLException {
        if (ctx.args.empty || ctx.rawArgs.equals("list")) {
            List<GuildRoleOption> options = dao.queryBuilder()
                    .where()
                    .eq("guildId", ctx.guild.getIdLong())
                    .query();

            if (options.size() > 0) {
                StringBuilder list = new StringBuilder();

                for (GuildRoleOption option: options) {
                    Role role = ctx.guild.getRoleById(option.getRoleId());

                    list.append("    \u2022 **")
                            .append(role.getName())
                            .append("**\n");
                }

                Color c = ctx.guild.getSelfMember().getColor();

                ctx.send(new EmbedBuilder()
                        .setAuthor("Roles", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                        .setColor(c == null ? Color.WHITE : c)
                        .setDescription(list.toString())
                        .build()).queue();
                return;
            } else {
                String h = ctx.member.hasPermission(Permission.MANAGE_ROLES) ?
                        "\n**Tip**: You can add role options with the `roles` command, because you have Manage Roles."
                        : "";
                ctx.send(Emotes.getFailure() + " This server doesn't have any roles available." + h).queue();
            }
        }

        List<Role> roles = ctx.guild.getRolesByName(ctx.rawArgs, true);
        if (roles.size() < 1) {
            ctx.fail("No such role! Use no arguments or `list` to list available roles.");
            return;
        }

        Role role = roles.get(0);
        GuildRoleOption option = dao.queryForId(role.getIdLong());
        GuildMemberActions prof = profDao.queryBuilder()
                .where()
                .eq("userId", ctx.author.getIdLong())
                .eq("guildId", ctx.guild.getIdLong())
                .queryForFirst();
        if (prof == null) {
            prof = new GuildMemberActions(ctx.author.getIdLong(), ctx.guild.getIdLong(), (short)0,
                    false, false);
            profDao.create(prof);
        }

        if (option == null || !option.test(ctx, prof.messagesSent, prof.hasBeenMentioned, prof.hasMentionedOther)) {
            ctx.fail("You can't get that role right now!");
            return;
        }

        ctx.guild.getController().addSingleRoleToMember(ctx.member, role).complete();

        ctx.success("Role added.");
    }

    @Perm.ManageRoles
    @Command(name = "roles", desc = "Manage roles that users can get.", thread = true,
            aliases = {"roleman", "role_manager", "rolecontrol", "rcontrol", "rman"})
    public void cmdControl(Context ctx) {

    }
}
