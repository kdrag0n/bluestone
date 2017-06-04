package com.khronodragon.bluestone;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Command {
    public final String name;
    public final String description;
    public final String usage;
    public final boolean hidden;
    public final String[] permsRequired;
    public final boolean guildOnly;
    public final String[] aliases;
    public final String cogName;
    public final boolean needThread;
    private List<Method> checks = new ArrayList<>();
    private final Method func;
    private final Cog instance;

    public Command(String name, String desc, String usage, boolean hidden,
                   String[] permsRequired, boolean guildOnly,
                   String[] aliases, Method func, Cog cogInstance, boolean needThread) {
        this.name = name;
        this.description = desc;
        this.usage = usage;
        this.hidden = hidden;
        this.permsRequired = permsRequired;
        this.guildOnly = guildOnly;
        this.aliases = aliases;
        this.func = func;
        this.instance = cogInstance;
        this.cogName = cogInstance.getName();
        this.needThread = needThread;
    }

    public void invoke(Bot bot, MessageReceivedEvent event, List<String> args,
                       String prefix, String invoker) throws IllegalAccessException, InvocationTargetException {
        Context context = new Context(bot, event, args, prefix, invoker);

        if (!this.runChecks(context)) {
            System.out.println("Checks failed for command " + this.name);
        }

        if (needThread) {
            Runnable task = () -> {
                try {
                    func.invoke(instance, context);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    event.getChannel().sendMessage(":x: A severe internal error occurred.").queue();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    Throwable cause = e.getCause();
                    if (cause == null) {
                        event.getChannel().sendMessage(":x: An unknown internal error occurred.").queue();
                    } else {
                        event.getChannel().sendMessage(String.format(":warning: Error in `%s%s`:```java\n%s```", prefix, invoker, bot.vagueTrace(cause))).queue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    event.getChannel().sendMessage(String.format(":warning: Error in `%s%s`:```java\n%s```", prefix, invoker, e.toString())).queue();
                }
            };

            bot.threadExecutor.execute(task);
        } else {
            func.invoke(instance, context);
        }
    }

    private boolean runChecks(Context context) throws IllegalAccessException, InvocationTargetException {
        for (Method method: this.checks) {
            if (!(boolean) method.invoke(null, context)) {
                return false;
            }
        }
        return true;
    }
}