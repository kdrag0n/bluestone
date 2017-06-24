package com.khronodragon.bluestone;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Permissions {
    public static boolean check(String[] permsRequired, Context ctx) {
        if (ctx.author.getIdLong() == ctx.bot.owner.getIdLong())
            return true;

        for (String perm: permsRequired) {
            if (perm.equals("owner")) {
                if (ctx.author.getIdLong() != ctx.bot.owner.getIdLong())
                    return false;
            } else if (perm.equals("admin")) {
                try {
                    if (!ctx.bot.getAdminDao().idExists(ctx.author.getIdLong()))
                        return false;
                } catch (SQLException e) {
                    ctx.bot.logger.warn("Bot admin perm check error", e);
                    return false;
                }
            } else {
                if (ctx.guild != null) {
                    String jdaPermStr = String.join("_", Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(perm))
                    .map(String::toUpperCase)
                    .collect(Collectors.toList()));

                    Permission jdaPerm;
                    try {
                        jdaPerm = Permission.valueOf(jdaPermStr);
                    } catch (IllegalArgumentException e) {
                        return false;
                    }

                    if (!ctx.member.hasPermission((Channel) ctx.channel, jdaPerm))
                        return false;
                }
            }
        }
        return true;
    }
}
