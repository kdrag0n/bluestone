package com.khronodragon.bluestone;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import org.apache.commons.lang3.StringUtils;
import sun.misc.Unsafe;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Permissions {
    private static final Unsafe unsafe = Bot.getUnsafe();
    public static final Permission BOT_OWNER;
    public static final Permission BOT_ADMIN;
    public static final Permission PATREON_SUPPORTER;

    static {
        Class<Permission> clazz = Permission.class;
        unsafe.allocateInstance(clazz)
    }

    public static Permission createPerm(int offset, boolean isGuild, boolean isChannel, String name) {
        try {

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean check(String[] permsAccepted, Context ctx) {
        if (ctx.author.getIdLong() == ctx.bot.owner.getIdLong())
            return true;

        for (String perm: permsAccepted) {
            if (perm.equals("owner")) {
                return false;
            } else if (perm.equals("admin")) {
                try {
                    if (ctx.bot.getAdminDao().idExists(ctx.author.getIdLong()))
                        return true;
                } catch (SQLException e) {
                    ctx.bot.logger.warn("Bot admin perm check error", e);
                }
            } else {
                if (ctx.guild != null) {
                    for (String cmpPerm: StringUtils.split(perm, '&')) {
                        String jdaPermStr = String.join("_", Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(cmpPerm))
                                .map(String::toUpperCase)
                                .collect(Collectors.toList()));

                        Permission jdaPerm;
                        try {
                            jdaPerm = Permission.valueOf(jdaPermStr);
                        } catch (IllegalArgumentException ignored) {
                            continue;
                        }

                        if (!ctx.member.hasPermission((Channel) ctx.channel, jdaPerm))
                            return false;
                    }

                    return true;
                }
            }
        }
        return false;
    }
}
