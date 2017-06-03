package com.khronodragon.bluestone;

import com.khronodragon.bluestone.annotations.Command;

import java.lang.reflect.Method;

public abstract class Cog implements ClassUtilities {
    public abstract String getName();
    public abstract String getDescription();
    private final Bot bot;

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
}
