package com.khronodragon.bluestone;

import com.khronodragon.bluestone.errors.CheckFailure;
import com.khronodragon.bluestone.errors.GuildOnlyError;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.errors.PermissionError;
import com.khronodragon.bluestone.util.Strings;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.khronodragon.bluestone.util.Strings.format;

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
    public final boolean reportErrors;
    public final boolean requiresOwner;
    private List<Predicate<Context>> checks = new ArrayList<>(1);
    private final Method func;
    public final Cog cog;

    public Command(String name, String desc, String usage, boolean hidden,
                   String[] permsRequired, boolean guildOnly, String[] aliases,
                   Method func, Cog cogInstance, boolean needThread, boolean reportErrors) {
        this.name = name;
        this.description = desc;
        this.usage = usage;
        this.hidden = hidden;
        this.permsRequired = permsRequired;
        this.guildOnly = guildOnly;
        this.aliases = aliases;
        this.func = func;
        this.cog = cogInstance;
        this.cogName = cogInstance.getName();
        this.needThread = needThread;
        this.reportErrors = reportErrors;
        this.requiresOwner = ArrayUtils.contains(permsRequired, "owner");
    }

    public void invoke(Bot bot, MessageReceivedEvent event, List<String> args,
                       String prefix, String invoker) throws IllegalAccessException, InvocationTargetException, CheckFailure {
        Context ctx = new Context(bot, event, args, prefix, invoker);

        runChecks(ctx);

        func.invoke(cog, ctx);
    }

    public void simpleInvoke(Bot bot, MessageReceivedEvent event, List<String> args,
                             String prefix, String invoker) {
        if (needThread) {
            Runnable task = () -> {
                invokeWithHandling(bot, event, args, prefix, invoker);
            };

            if (bot.threadExecutor.getActiveCount() >= bot.threadExecutor.getMaximumPoolSize()) {
                event.getChannel().sendMessage(
                        "⌛ Your command has been queued. **If this happens often, contact the owner!**").queue();
            }
            bot.threadExecutor.execute(task);
        } else {
            invokeWithHandling(bot, event, args, prefix, invoker);
        }
    }

    public void invokeWithHandling(Bot bot, MessageReceivedEvent event, List<String> args,
                                   String prefix, String invoker) {
        MessageChannel channel = event.getChannel();

        try {
            try {
                invoke(bot, event, args, prefix, invoker);
            } catch (IllegalAccessException e) {
                bot.logger.error("Severe command ({}) invocation error:", invoker, e);
                channel.sendMessage(Emotes.getFailure() + " A severe internal error occurred.").queue();
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();

                if (cause == null) {
                    bot.logger.error("Unknown command ({}) invocation error:", invoker, e);
                    channel.sendMessage(Emotes.getFailure() + " An unknown internal error occurred.").queue();
                } else if (cause instanceof PassException) {
                    // assume error has already been sent
                } else if (cause instanceof PermissionError) {
                    channel.sendMessage(format("{0} Missing permission for `{1}{2}`! **{3}** will work.",
                            event.getAuthor().getAsMention(), prefix, invoker,
                            Strings.smartJoin(((PermissionError) cause).getFriendlyPerms(), "or"))).queue();
                } else if (cause instanceof PermissionException) {
                    channel.sendMessage(Emotes.getFailure() + " I need the **" +
                            ((PermissionException) cause).getPermission().getName() + "** permission!").queue();
                } else if (cause instanceof SQLException) {
                    bot.logger.error("SQL error in command {}:", invoker, cause);
                    channel.sendMessage(format(Emotes.getFailure() + " A database error has occurred.```java\n{2}```This error has been reported.",
                            prefix, invoker, Bot.briefSqlError(((SQLException) cause)))).queue();

                    bot.reportErrorToOwner(cause, event.getMessage(), this);
                } else {
                    bot.logger.error("Command ({}) invocation error:", invoker, cause);
                    channel.sendMessage(format(Emotes.getFailure() + " Error!```java\n{2}```This error has been reported.",
                            prefix, invoker, Bot.vagueTrace(cause))).queue();

                    if (reportErrors)
                        bot.reportErrorToOwner(cause, event.getMessage(), this);
                }
            } catch (PermissionError e) {
                channel.sendMessage(format("{0} Missing permission for `{1}{2}`! **{3}** will work.",
                        event.getAuthor().getAsMention(), prefix, invoker,
                        Strings.smartJoin(e.getFriendlyPerms(), "or"))).queue();
            } catch (GuildOnlyError e) {
                channel.sendMessage(Emotes.getFailure() + "Sorry, that command only works in a server.").queue();
            } catch (CheckFailure e) {
                channel.sendMessage(format("{0} A check for `{1}{2}` failed. Do you not have permissions?",
                        event.getAuthor().getAsMention(), prefix, invoker)).queue();
            } catch (Exception e) {
                bot.logger.error("Unknown command ({}) error:", invoker, e);
                channel.sendMessage(Emotes.getFailure() + " A severe internal error occurred. The error has been reported.").queue();

                bot.reportErrorToOwner(e, event.getMessage(), this);
            }
        } catch (PermissionException ignored) {}
    }

    private boolean runChecks(Context ctx) throws CheckFailure {
        if (guildOnly && ctx.guild == null) {
            throw new GuildOnlyError("Command only works in a guild");
        }

        if (permsRequired.length > 0) {
            checkPerms(ctx);
        }

        for (Predicate<Context> check: checks) {
            try {
                if (!check.test(ctx)) {
                    throw new CheckFailure("Check failed");
                }
            } catch (CheckFailure e) {
                throw e;
            } catch (Exception e) {
                throw new CheckFailure("Check failed");
            }
        }
        return true;
    }

    private void checkPerms(Context ctx) throws PermissionError {
        if (!Permissions.check(permsRequired, ctx))
            throw new PermissionError("Requester missing permissions for command " + name)
                   .setPerms(permsRequired);
    }

    public static void checkPerms(Context ctx, String[] permsRequired) {
        if (!Permissions.check(permsRequired, ctx))
            throw new PermissionError("Requester missing permissions for command")
                    .setPerms(permsRequired);
    }
}