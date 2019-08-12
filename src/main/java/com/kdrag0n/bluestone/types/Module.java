package com.kdrag0n.bluestone.types;

import com.j256.ormlite.dao.Dao;
import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.Context;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Module {
    protected final Bot bot;
    public abstract String getName();

    public Module(Bot bot) {
        this.bot = bot;
    }

    public String getDisplayName() {
        return getName();
    }

    public void onLoad() {}

    protected static int getRandInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    protected static boolean stringExists(String string, String... options) {
        for (String opt: options) {
            if (string.equals(opt)) {
                return true;
            }
        }
        return false;
    }

    protected static Color randomColor() { // 255**3 = 16581375
        return new Color(getRandInt(0, 16581375));
    }

    protected static EmbedBuilder newEmbedWithAuthor(Context ctx) {
        return newEmbedWithAuthor(ctx, null);
    }

    @SuppressWarnings("SameParameterValue")
    protected static EmbedBuilder newEmbedWithAuthor(Context ctx, String url) {
        String name;
        String iconUrl;

        if (ctx.guild != null) {
            Member me = ctx.guild.getSelfMember();
            name = me.getEffectiveName();
            iconUrl = me.getUser().getEffectiveAvatarUrl();
        } else {
            User me = ctx.jda.getSelfUser();
            name = me.getName();
            iconUrl = me.getEffectiveAvatarUrl();
        }

        return new EmbedBuilder()
                .setAuthor(name, url, iconUrl);
    }

    protected static String getEffectiveName(Context ctx) {
        if (ctx.guild == null) {
            return ctx.jda.getSelfUser().getName();
        } else {
            return ctx.guild.getSelfMember().getEffectiveName();
        }
    }

    public static String getTag(User user) {
        return user.getName() + '#' + user.getDiscriminator();
    }

    protected static <T> T randomChoice(T[] array) {
        return array[getRandInt(0, array.length)];
    }

    @SuppressWarnings("SameParameterValue")
    protected static <T> T randomChoice(List<T> list) {
        return list.get(getRandInt(0, list.size()));
    }

    protected static String[] embedFieldPages(String text) {
        return StringUtils.split(WordUtils.wrap(text, 1024, "||", true, "\\s+"), "||");
    }

    @Nullable
    @CheckReturnValue
    protected static TextChannel defaultWritableChannel(Member member) {
        return member.getGuild().getTextChannelCache().streamUnordered()
                .filter(c -> member.hasPermission(c, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    @Nullable
    @CheckReturnValue
    protected static TextChannel defaultReadableChannel(Member member) {
        return member.getGuild().getTextChannelCache().streamUnordered()
                .filter(c -> member.hasPermission(c, Permission.MESSAGE_READ))
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    @CheckReturnValue
    protected static long parseSnowflake(String input) {
        try {
            return MiscUtil.parseSnowflake(input);
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    @CheckReturnValue
    protected<C, K> Dao<C, K> setupDao(Class<C> clazz) {
        return bot.setupDao(clazz);
    }
}
