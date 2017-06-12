package com.khronodragon.bluestone;

import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.apache.logging.log4j.LogManager;

import java.awt.Color;
import java.lang.reflect.Method;

public abstract class Cog implements ClassUtilities {
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

    protected Color randomColor() {
        return new Color(randint(0, (int) Math.pow(255, 3) - 1));
    }

    protected EmbedBuilder newEmbedWithAuthor(Context ctx) {
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

    protected EmbedBuilder newEmbedWithAuthor(Context ctx, String url) {
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

    public static void removeReactionIfExists(Message message, String unicode) {
        message.getReactions().stream().filter(r -> r.getEmote().getName().equals(unicode)).forEach(r -> {
            r.removeReaction().queue();
        });
    }
}
