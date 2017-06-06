package com.khronodragon.bluestone;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Permissions {
    public static boolean check(String[] permsRequired, Context ctx) {
        for (String perm: permsRequired) {
            if (perm.equals("owner")) {
                if (ctx.author.getIdLong() != ctx.bot.owner.getIdLong()) {
                    return false;
                }
            } else if (perm.equals("admin")) {
//                if (!ctx.bot.store.get("admins").contains(ctx.author.getIdLong())) {
//                    return false;
//                }
            } else {
                if (ctx.guild != null) {
                    String jdaPermStr = String.join("_", Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(perm))
                    .map(String::toUpperCase)
                    .collect(Collectors.toList()));
                    Permission jdaPerm = Permission.valueOf(jdaPermStr);

                    if (jdaPerm == null) {
                        return false;
                    }

                    if (!ctx.member.hasPermission((Channel) ctx.channel, jdaPerm)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
