package com.khronodragon.bluestone;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;

import java.awt.Color;
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

    public void load() {
        LogManager.getLogger(this.getClass()).info("Cog loaded.", getName());
    }

    public void unload() {
        LogManager.getLogger(this.getClass()).info("Cog unloaded.", getName());
    }

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

    public static Color randomColor() { // 255**3 = 16581375
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
            if (r.getEmote().getName().equals(unicode)) {
                r.removeReaction().queue();
                break;
            }
        }
    }

    protected static <T> T randomChoice(T[] array) {
        return array[randint(0, array.length)];
    }

    protected static <T> T randomChoice(List<T> list) {
        return list.get(randint(0, list.size()));
    }

    protected static String[] embedFieldPages(String text) {
        return StringUtils.split(WordUtils.wrap(text, 1024, "||", true, "\\s+"), "||");
    }
}
