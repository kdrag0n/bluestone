package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.sql.GuildRoleOption;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class RolemanCog extends Cog {
    private static final Logger logger = LogManager.getLogger(RolemanCog.class);
    private Dao<GuildRoleOption, Long> dao;

    public RolemanCog(Bot bot) {
        super(bot);

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), GuildRoleOption.class);
        } catch (SQLException e) {
            logger.warn("Failed to create role option table!", e);
        }

        try {
            dao = DaoManager.createDao(bot.getShardUtil().getDatabase(), GuildRoleOption.class);
        } catch (SQLException e) {
            logger.warn("Failed to create role option DAO!", e);
        }
    }

    public String getName() {
        return "Role Manager";
    }

    public String getDescription() {
        return "The role manager that allows users to get roles.";
    }

    @Command(name = "role", desc = "Get or remove a role.", aliases = {"rank"}, usage = "[role name]",
            thread = true, guildOnly = true)
    public void cmdRole(Context ctx) throws SQLException {
        if (ctx.rawArgs.length() < 1 || ctx.rawArgs.equals("list")) {
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
                ctx.send(Emotes.getFailure() + " This server doesn't have any roles available.").queue();
            }
        }

        List<Role> roles = ctx.guild.getRolesByName(ctx.rawArgs, true);
        if (roles.size() < 1) {
            ctx.send(Emotes.getFailure() + " No such role! Use no arguments or `list` to list available roles.").queue();
            return;
        }

        Role role = roles.get(0);
        GuildRoleOption option = dao.queryForId(role.getIdLong());

        if (option == null || !option.test(ctx)) {

        }
    }
}
