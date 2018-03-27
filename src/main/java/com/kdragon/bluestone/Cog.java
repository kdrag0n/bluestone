package com.kdragon.bluestone;

import com.j256.ormlite.dao.Dao;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Cog {
    protected final Bot bot;
    public abstract String getName();
    public abstract String getDescription();

    public Cog(Bot bot) {
        this.bot = bot;
    }

    public String getCosmeticName() {
        return getName();
    }

    public void load() {}

    public void unload() {}

    protected static int randint(int min, int max) {
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
        return new Color(randint(0, 16581375));
    }

    protected static EmbedBuilder newEmbedWithAuthor(Context ctx) {
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
                .setAuthor(name, null, iconUrl);
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

    protected static String getEffectiveName(User user, Guild guild) {
        if (guild == null) {
            return user.getName();
        } else {
            Member member = guild.getMember(user);
            if (member == null)
                return user.getName();

            return member.getEffectiveName();
        }
    }

    public static String getTag(User user) {
        return user.getName() + '#' + user.getDiscriminator();
    }

    public static void removeReactionIfExists(Message message, String unicode) {
        for (MessageReaction r: message.getReactions()) {
            if (r.getReactionEmote().getName().equals(unicode)) {
                r.removeReaction().queue();
                break;
            }
        }
    }

    protected static <T> T randomChoice(T[] array) {
        return array[randint(0, array.length)];
    }

    @SuppressWarnings("SameParameterValue")
    protected static <T> T randomChoice(List<T> list) {
        return list.get(randint(0, list.size()));
    }

    protected static String[] embedFieldPages(String text) {
        return StringUtils.split(WordUtils.wrap(text, 1024, "||", true, "\\s+"), "||");
    }

    @Nullable
    @CheckReturnValue
    protected static TextChannel defaultWritableChannel(Member member) {
        return ((GuildImpl) member.getGuild()).getTextChannelsMap().valueCollection().stream()
                .sorted(Comparator.naturalOrder())
                .filter(c -> member.hasPermission(c, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
                .findFirst().orElse(null);
    }

    @Nullable
    @CheckReturnValue
    protected static TextChannel defaultReadableChannel(Member member) {
        return ((GuildImpl) member.getGuild()).getTextChannelsMap().valueCollection().stream()
                .sorted(Comparator.naturalOrder())
                .filter(c -> member.hasPermission(c, Permission.MESSAGE_READ))
                .findFirst().orElse(null);
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
        return bot.shardUtil.setupDao(clazz);
    }
}
