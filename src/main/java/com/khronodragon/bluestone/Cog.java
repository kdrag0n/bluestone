package com.khronodragon.bluestone;

import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.lang.reflect.Method;

public abstract class Cog implements ClassUtilities {
    public abstract String getName();
    public abstract String getDescription();
    protected final Bot bot;

    public Cog(Bot bot) {
        this.bot = bot;
    }

    public void load() {
        printf("[%s] Cog loaded.", getName());
    }

    public void unload() {
        printf("[%s] Cog unloaded.", getName());
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
    }

    protected Color randomColor() {
        return new Color(randint(0, (int) Math.pow(255, 3) - 1));
    }

    protected EmbedBuilder newEmbedWithAuthor(Context ctx) {
        String name;
        String url = "";
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
}
