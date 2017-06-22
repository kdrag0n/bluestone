package com.khronodragon.bluestone;

import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.util.ClassUtilities;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Cog {
    public abstract String getName();
    public abstract String getDescription();
    protected final Bot bot;

    public Cog(Bot bot) {
        this.bot = bot;
    }

    public void load() {
        LogManager.getLogger(this.getClass()).info("Cog loaded.", getName());
    }

    public void unload() {
        LogManager.getLogger(this.getClass()).info("[%s] Cog unloaded.", getName());
    }

    public void register() {
        Class obj = this.getClass();

        for (Method method: obj.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command anno = method.getAnnotation(Command.class);

                com.khronodragon.bluestone.Command command = new com.khronodragon.bluestone.Command(
                        anno.name(), anno.desc(), anno.usage(), anno.hidden(),
                        anno.perms(), anno.guildOnly(), anno.aliases(), method, this,
                        anno.thread()
                );

                bot.commands.put(command.name, command);
                for (String al: command.aliases) {
                    bot.commands.put(al, command);
                }
            }
        }

        bot.cogs.put(this.getName(), this);

        if (this instanceof EventedCog) {
            bot.eventedCogs.add((EventedCog) this);
        }
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

    public static Color randomColor() {
        return new Color(randint(0, (int) Math.pow(255, 3) - 1));
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

    public static String getTag(User user) {
        return user.getName() + '#' + user.getDiscriminator();
    }

    public static void removeReactionIfExists(Message message, String unicode) {
        message.getReactions().stream().filter(r -> r.getEmote().getName().equals(unicode)).forEach(r -> {
            r.removeReaction().queue();
        });
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
