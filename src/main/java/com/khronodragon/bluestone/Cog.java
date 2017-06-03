package com.khronodragon.bluestone;

import com.khronodragon.bluestone.annotations.Command;

import java.lang.reflect.Method;

public interface Cog {
    String getName();
    String getDescription();
    Bot bot;

    default void print(String text) {
        System.out.println(text);
    }

    default void printf(String fmt, Object... args) {
        System.out.printf(fmt, args);
    }

    default void load() {
        printf("[%s] Cog loaded.", getName());
    }

    default void unload() {
        printf("[%s] Cog unloaded.", getName());
    }

    default void register() {
        Class obj = this.getClass();

        for (Method method: obj.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command anno = method.getAnnotation(Command.class);

                com.khronodragon.bluestone.Command command = new com.khronodragon.bluestone.Command(
                        anno.name(), anno.description(), anno.usage(), anno.hidden(),
                        anno.perms(), anno.guildOnly(), anno.aliases(), method, this
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
