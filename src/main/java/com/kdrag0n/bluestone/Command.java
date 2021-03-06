package com.kdrag0n.bluestone;

import com.kdrag0n.bluestone.errors.GuildOnlyException;
import com.kdrag0n.bluestone.errors.PassException;
import com.kdrag0n.bluestone.errors.PermissionException;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.types.Perm;
import com.kdrag0n.bluestone.util.ArrayListView;
import com.kdrag0n.bluestone.util.Strings;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.JSONException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

import static java.lang.String.format;

public class Command {
    public final String name;
    public final String description;
    public final String usage;
    public final boolean hidden;
    private final EnumSet<Perm> permsRequired;
    private final boolean guildOnly;
    public final String[] aliases;
    public final boolean requiresOwner;
    private final Method func;
    public final Module module;

    public Command(String name, String desc, String usage, boolean hidden,
                   EnumSet<Perm> permsRequired, boolean guildOnly, String[] aliases,
                   Method func, Module moduleInstance) {
        this.name = name;
        this.description = desc;
        this.usage = usage;
        this.hidden = hidden;
        this.permsRequired = permsRequired;
        this.guildOnly = guildOnly;
        this.aliases = aliases;
        this.func = func;
        this.module = moduleInstance;
        this.requiresOwner = permsRequired.contains(Perm.BOT_OWNER);
    }

    private void invoke(Bot bot, MessageReceivedEvent event, ArrayListView args,
                        String prefix, String invoker, String content, boolean processArgs)
            throws IllegalAccessException, InvocationTargetException {
        Context ctx = new Context(bot, event, args, prefix, invoker, content, processArgs);

        if (guildOnly && ctx.guild == null) {
            throw new GuildOnlyException("Command only works in guilds");
        }

        if (permsRequired.size() != 0) {
            Perm.checkThrow(ctx, permsRequired);
        }

        func.invoke(module, ctx);
    }

    /*package-private*/ void simpleInvoke(Bot bot, MessageReceivedEvent event, ArrayListView args,
                             String prefix, String invoker, String content, boolean processArgs) {
        Runnable task = () -> invokeWithHandling(bot, event, args, prefix, invoker, content, processArgs);

        if (Bot.threadExecutor.getActiveCount() >= Bot.threadExecutor.getMaximumPoolSize()) {
            event.getChannel().sendMessage(
                    "⌛ Your command has been queued.").queue();
        }

        Bot.threadExecutor.execute(task);
    }

    private void invokeWithHandling(Bot bot, MessageReceivedEvent event, ArrayListView args,
                                    String prefix, String invoker, String content, boolean processArgs) {
        MessageChannel channel = event.getChannel();

        try {
            try {
                invoke(bot, event, args, prefix, invoker, content, processArgs);
            } catch (IllegalAccessException e) {
                Bot.logger.error("Severe command ({}) invocation error:", invoker, e);
                channel.sendMessage(Emotes.getFailure() + " An internal error occurred.").queue();
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();

                if (cause == null) {
                    Bot.logger.error("Unknown command ({}) invocation error:", invoker, e);
                    channel.sendMessage(Emotes.getFailure() + " An unknown internal error occurred.").queue();
                } else //noinspection StatementWithEmptyBody
                    if (cause instanceof PassException) {
                    // assume error has already been sent
                } else if (cause instanceof PermissionException) {
                    channel.sendMessage(format("You can't use `%s` because **%s** is required.",
                            invoker, Strings.smartJoin(((PermissionException) cause).getFriendlyPerms(), "or"))).queue();
                } else if (cause instanceof net.dv8tion.jda.api.exceptions.PermissionException) {
                    channel.sendMessage(Emotes.getFailure() + " I need the **" +
                            ((net.dv8tion.jda.api.exceptions.PermissionException) cause).getPermission().getName() + "** permission.").queue();
                } else if (cause instanceof ErrorResponseException) {
                    if (((ErrorResponseException) cause).getErrorCode() == 50013) {
                        channel.sendMessage(Emotes.getFailure() + " I don't have the permission to do that.").queue();
                    } else {
                        Bot.logger.error("Command ({}) invocation error:", invoker, cause);
                        channel.sendMessage(format(Emotes.getFailure() + " An error occurred. `%s`",
                                cause.getClass().getSimpleName())).queue();
                    }
                } else if (cause instanceof SQLException) {
                    Bot.logger.error("SQL error in command {}:", invoker, cause);
                    channel.sendMessage(Emotes.getFailure() + " A database error occurred.").queue();
                } else if (cause instanceof JSONException) {
                    Bot.logger.error("Command {}: Invalid JSON received", invoker);
                    channel.sendMessage(Emotes.getFailure() + " The service provided invalid data. Try again later.").queue();
                } else {
                    Bot.logger.error("Command ({}) invocation error:", invoker, cause);
                    channel.sendMessage(format(Emotes.getFailure() + " An error occurred. `%s`",
                            cause.getClass().getSimpleName())).queue();
                }
            } catch (PermissionException e) {
                channel.sendMessage(format("You can't use `%s` because **%s** is required.",
                        invoker, Strings.smartJoin(e.getFriendlyPerms(), "or"))).queue();
            } catch (GuildOnlyException e) {
                channel.sendMessage(Emotes.getFailure() + " That command only works in a server.").queue();
            } catch (Exception e) {
                Bot.logger.error("Unknown command ({}) error:", invoker, e);
                channel.sendMessage(Emotes.getFailure() + " An internal error occurred.").queue();
            }
        } catch (net.dv8tion.jda.api.exceptions.PermissionException ignored) {}
    }
}