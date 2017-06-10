package com.khronodragon.bluestone;

import com.khronodragon.bluestone.errors.CheckFailure;
import com.khronodragon.bluestone.errors.GuildOnlyError;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.errors.PermissionError;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    public final Cog instance;

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
                       String prefix, String invoker) throws IllegalAccessException, InvocationTargetException, CheckFailure {
        Context ctx = new Context(bot, event, args, prefix, invoker);

        runChecks(ctx);

        if (needThread) {
            Runnable task = () -> {
                try {
                    func.invoke(instance, ctx);
                } catch (IllegalAccessException e) {
                    bot.logger.error("Severe command ({}) invocation error:", invoker, e);
                    event.getChannel().sendMessage(":x: A severe internal error occurred.").queue();
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause == null) {
                        bot.logger.error("Unknown command ({}) invocation error:", invoker, e);
                        event.getChannel().sendMessage(":x: An unknown internal error occurred.").queue();
                    } else if (cause instanceof PassException) {
                    } else {
                        bot.logger.error("Command ({}) invocation error:", invoker, cause);
                        event.getChannel().sendMessage(String.format(":warning: Error in `%s%s`:```java\n%s```", prefix, invoker, bot.vagueTrace(cause))).queue();
                    }
                } catch (PermissionError e) {
                    event.getChannel().sendMessage(String.format("%s Not enough permissions for `%s%s`! **%s** will work.", event.getAuthor().getAsMention(), prefix, invoker,
                            Strings.smartJoin(permsRequired))).queue();
                } catch (GuildOnlyError e) {
                    event.getChannel().sendMessage("Sorry, that command only works in a guild.").queue();
                } catch (CheckFailure e) {
                    bot.logger.error("Checks failed for command {}:", invoker);
                    event.getChannel().sendMessage(String.format("%s A check for `%s%s` failed. Do you not have permissions?", event.getAuthor().getAsMention(), prefix, invoker)).queue();
                } catch (Exception e) {
                    bot.logger.error("Unknown command ({}) error:", invoker, e);
                    event.getChannel().sendMessage(String.format(":warning: Error in `%s%s`:```java\n%s```", prefix, invoker, e.toString())).queue();
                }
            };

            bot.threadExecutor.execute(task);
        } else {
            func.invoke(instance, ctx);
        }
    }

    private boolean runChecks(Context ctx) throws CheckFailure {
        if (guildOnly) {
            if (ctx.guild == null) {
                throw new GuildOnlyError("Command only works in a guild");
            }
        }

        if (permsRequired.length > 0) {
            checkPerms(ctx);
        }

        for (Method method: this.checks) {
            try {
                if (!(boolean) method.invoke(null, ctx)) {
                    throw new CheckFailure("Check " + method.getName() + "() failed");
                }
            } catch (CheckFailure e) {
                throw e;
            } catch (Exception e) {
                throw new CheckFailure("Check " + method.getName() + "() failed");
            }
        }
        return true;
    }

    private void checkPerms(Context ctx) throws PermissionError {
        if (!Permissions.check(permsRequired, ctx)) {
            throw new PermissionError("Requester missing permissions for command " + name);
        }
    }

    public String[] getFriendlyPerms() {
        return Arrays.stream(permsRequired).map(p -> {
            if (p.equals("owner")) {
                return "Bot Owner";
            } else if (p.equals("admin")) {
                return "Bot Admin";
            } else {
                String jdaPermStr = String.join("_",
                        Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(p))
                                .map(String::toUpperCase)
                                .collect(Collectors.toList()));
                Permission perm = Permission.valueOf(jdaPermStr);
                if (perm == null) {
                    return "Unknown";
                } else {
                    return perm.getName();
                }
            }
        }).collect(Collectors.toList()).toArray(new String[0]);
    }
}