package com.kdragon.bluestone;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Permissions {
    private static final Unsafe unsafe = Bot.getUnsafe();
    private static final Class<Permission> permClass = Permission.class;
    private static final Field permOffset, permRaw, permIsGuild, permIsChannel, permName;
    public static final Permission BOT_OWNER;
    public static final Permission BOT_ADMIN;
    public static final Permission PATREON_SUPPORTER;
    private static final Permission COMPOUND;
    public static final Map<Permission, Permission[]> compoundMap =
            new ConcurrentHashMap<>(128, 0.85f, 8);

    static {
        try {
            permOffset = permClass.getDeclaredField("offset");
            permRaw = permClass.getDeclaredField("raw");
            permIsGuild = permClass.getDeclaredField("isGuild");
            permIsChannel = permClass.getDeclaredField("isChannel");
            permName = permClass.getDeclaredField("name");

            permOffset.setAccessible(true);
            permRaw.setAccessible(true);
            permIsGuild.setAccessible(true);
            permIsChannel.setAccessible(true);
            permName.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        BOT_OWNER = createPerm(61, false, false, "Bot Owner");
        BOT_ADMIN = createPerm(60, false, false, "Bot Admin");
        PATREON_SUPPORTER = createPerm(59, false, false, "Patreon Supporter");
        COMPOUND = createPerm(58, false, false, "Compound Permission");
    }

    public static Permission createPerm(int offset, boolean isGuild, boolean isChannel, String name) {
        try {
            Permission perm = (Permission) unsafe.allocateInstance(permClass);

            permOffset.setInt(perm, offset);
            permRaw.setLong(perm, 1 << offset);
            permIsGuild.setBoolean(perm, isGuild);
            permIsChannel.setBoolean(perm, isChannel);
            permName.set(perm, name);

            return perm;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Permission setRaw(Permission perm, long raw) {
        try {
            permRaw.setLong(perm, raw);
            return perm;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean check(Context ctx, Permission... permsAccepted) {
        if (ctx.author.getIdLong() == Bot.ownerId)
            return true;

        outerLoop:
        for (Permission perm: permsAccepted) {
            if (perm == BOT_OWNER) {
            } else if (perm == BOT_ADMIN) {
                try {
                    if (ctx.bot.getAdminDao().idExists(ctx.author.getIdLong()))
                        return true;
                } catch (SQLException e) {
                    ctx.bot.logger.warn("Bot admin perm check error", e);
                }
            } else if (perm == PATREON_SUPPORTER) {
                if (Bot.patronIds.contains(ctx.author.getIdLong()))
                    return true;
            } else if (perm.getOffset() == 58) {
                Permission[] pA = compoundMap.get(perm);
                if (pA == null) {
                    ctx.bot.logger.error("Compound permission found, but not in compoundMap!");
                    continue;
                }

                for (Permission p: pA) {
                    if (p == BOT_OWNER) {
                        continue outerLoop;
                    } else if (p == BOT_ADMIN) {
                        try {
                            if (!ctx.bot.getAdminDao().idExists(ctx.author.getIdLong()))
                                continue outerLoop;
                        } catch (SQLException e) {
                            ctx.bot.logger.warn("Bot admin perm check error", e);
                        }
                    } else if (p == PATREON_SUPPORTER) {
                        if (!Bot.patronIds.contains(ctx.author.getIdLong()))
                            continue outerLoop;
                    } else {
                        if (ctx.guild != null) {
                            if (!ctx.member.hasPermission((TextChannel) ctx.channel, p))
                                continue outerLoop;
                        }
                    }
                }

                return true;
            } else {
                if (ctx.guild != null) {
                    if (ctx.member.hasPermission((TextChannel) ctx.channel, perm))
                        return true;
                }
            }
        }

        return false;
    }
}
