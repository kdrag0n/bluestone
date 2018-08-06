package com.kdrag0n.bluestone;

import com.kdrag0n.bluestone.errors.GuildOnlyError;
import com.kdrag0n.bluestone.errors.PassException;
import com.kdrag0n.bluestone.errors.PermissionError;
import com.kdrag0n.bluestone.util.ArrayListView;
import com.kdrag0n.bluestone.util.Strings;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

import static java.lang.String.format;

public class Command {
    public final String name;
    public final String description;
    public final String usage;
    public final boolean hidden;
    private final Permission[] permsRequired;
    private final boolean guildOnly;
    public final String[] aliases;
    private final boolean needThread;
    public final boolean requiresOwner;
    private final Method func;
    public final com.kdrag0n.bluestone.Cog cog;

    public Command(String name, String desc, String usage, boolean hidden,
                   Permission[] permsRequired, boolean guildOnly, String[] aliases,
                   Method func, com.kdrag0n.bluestone.Cog cogInstance, boolean needThread, boolean reportErrors) {
        this.name = name;
        this.description = desc;
        this.usage = usage;
        this.hidden = hidden;
        this.permsRequired = permsRequired;
        this.guildOnly = guildOnly;
        this.aliases = aliases;
        this.func = func;
        this.cog = cogInstance;
        this.needThread = needThread;
        this.requiresOwner = ArrayUtils.contains(permsRequired, Permissions.BOT_OWNER);
    }

    private void invoke(com.kdrag0n.bluestone.Bot bot, MessageReceivedEvent event, ArrayListView args,
                        String prefix, String invoker) throws IllegalAccessException, InvocationTargetException {
        com.kdrag0n.bluestone.Context ctx = new com.kdrag0n.bluestone.Context(bot, event, args, prefix, invoker);

        if (guildOnly && ctx.guild == null) {
            throw new GuildOnlyError("Command only works in a guild");
        }

        if (permsRequired.length > 0) {
            checkPerms(ctx);
        }

        func.invoke(cog, ctx);
    }

    public void simpleInvoke(com.kdrag0n.bluestone.Bot bot, MessageReceivedEvent event, ArrayListView args,
                             String prefix, String invoker) {
        if (needThread) {
            Runnable task = () -> invokeWithHandling(bot, event, args, prefix, invoker);

            if (com.kdrag0n.bluestone.Bot.threadExecutor.getActiveCount() >= com.kdrag0n.bluestone.Bot.threadExecutor.getMaximumPoolSize()) {
                event.getChannel().sendMessage(
                        "⌛ Your command has been queued.").queue();
            }
            com.kdrag0n.bluestone.Bot.threadExecutor.execute(task);
        } else {
            invokeWithHandling(bot, event, args, prefix, invoker);
        }
    }

    private void invokeWithHandling(com.kdrag0n.bluestone.Bot bot, MessageReceivedEvent event, ArrayListView args,
                                    String prefix, String invoker) {
        MessageChannel channel = event.getChannel();

        try {
            try {
                invoke(bot, event, args, prefix, invoker);
            } catch (IllegalAccessException e) {
                bot.logger.error("Severe command ({}) invocation error:", invoker, e);
                channel.sendMessage(Emotes.getFailure() + " An internal error occurred.").queue();
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();

                if (cause == null) {
                    bot.logger.error("Unknown command ({}) invocation error:", invoker, e);
                    channel.sendMessage(Emotes.getFailure() + " An unknown internal error occurred.").queue();
                } else //noinspection StatementWithEmptyBody
                    if (cause instanceof PassException) {
                    // assume error has already been sent
                } else if (cause instanceof PermissionError) {
                    channel.sendMessage(format("You can't use `%s` because **%s** is required.",
                            invoker, Strings.smartJoin(((PermissionError) cause).getFriendlyPerms(), "or"))).queue();
                } else if (cause instanceof PermissionException) {
                    channel.sendMessage(Emotes.getFailure() + " I need the **" +
                            ((PermissionException) cause).getPermission().getName() + "** permission.").queue();
                } else if (cause instanceof ErrorResponseException) {
                    if (((ErrorResponseException) cause).getErrorCode() == 50013) {
                        channel.sendMessage(Emotes.getFailure() + " I don't have the permission to do that.").queue();
                    } else {
                        bot.logger.error("Command ({}) invocation error:", invoker, cause);
                        channel.sendMessage(format(Emotes.getFailure() + " An error occurred. `%s`",
                                cause.getClass().getSimpleName())).queue();
                    }
                } else if (cause instanceof SQLException) {
                    bot.logger.error("SQL error in command {}:", invoker, cause);
                    channel.sendMessage(Emotes.getFailure() + " A database error occurred.").queue();
                } else if (cause instanceof JSONException) {
                    bot.logger.error("Command {}: Invalid JSON received", invoker);
                    channel.sendMessage(Emotes.getFailure() + " The service provided invalid data. Try again later.").queue();
                } else {
                    bot.logger.error("Command ({}) invocation error:", invoker, cause);
                    channel.sendMessage(format(Emotes.getFailure() + " An error occurred. `%s`",
                            cause.getClass().getSimpleName())).queue();
                }
            } catch (PermissionError e) {
                channel.sendMessage(format("You can't use `%s` because **%s** is required.",
                        invoker, Strings.smartJoin(e.getFriendlyPerms(), "or"))).queue();
            } catch (GuildOnlyError e) {
                channel.sendMessage(Emotes.getFailure() + " That command only works in a server.").queue();
            } catch (Exception e) {
                bot.logger.error("Unknown command ({}) error:", invoker, e);
                channel.sendMessage(Emotes.getFailure() + " An internal error occurred.").queue();
            }
        } catch (PermissionException ignored) {}
    }

    private void checkPerms(com.kdrag0n.bluestone.Context ctx) throws PermissionError {
        if (!Permissions.check(ctx, permsRequired))
            throw new PermissionError("Sender missing permissions for command " + name)
                   .setPerms(permsRequired);
    }

    public static void checkPerms(com.kdrag0n.bluestone.Context ctx, Permission[] permsRequired) {
        if (!Permissions.check(ctx, permsRequired))
            throw new PermissionError("Sender missing permissions")
                    .setPerms(permsRequired);
    }
}