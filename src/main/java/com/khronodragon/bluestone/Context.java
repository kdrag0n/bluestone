package com.khronodragon.bluestone;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.CheckReturnValue;
import java.util.List;

public class Context {
    public Bot bot;
    public final MessageReceivedEvent event;
    public final Message message;
    public User author;
    public final Guild guild;
    public final MessageChannel channel;
    public final Member member;
    public JDA jda;
    public final String prefix;
    public final List<String> args;
    public String invoker;
    public final String rawArgs;
    public final String mention;
    public boolean _flag = false;

    public Context(Bot bot, MessageReceivedEvent event, List<String> args,
                   String prefix, String invoker) {
        this.bot = bot;
        this.event = event;
        this.message = event.getMessage();
        this.author = event.getAuthor();
        this.guild = event.getGuild();
        this.channel = event.getChannel();
        this.member = event.getMember();
        this.jda = event.getJDA();
        this.prefix = prefix;
        this.args = args;
        this.invoker = invoker;
        this.mention = author.getAsMention();
        this.rawArgs = message.getContentRaw().substring(Math.min(prefix.length() + invoker.length() + 1, prefix.length() + invoker.length())).trim();
    }

    public static String truncate(String msg) {
        return truncate(msg, "**...too long**");
    }

    public static String truncate(String msg, String truncateString) {
        if (msg.length() > 2000) {
            msg = msg.substring(0, 2000);
            if (StringUtils.countMatches(msg, "```") % 2 == 1) {
                truncateString = "```" + truncateString;
            }
            return msg.substring(0, msg.length() - truncateString.length()) + truncateString;
        } else {
            return msg;
        }
    }

    public static String filterMessage(String msg) {
        return truncate(StringUtils.replace(StringUtils.replace(msg, "@everyone", "@\u200beveryone")
                , "@here", "@\u200bhere"));
    }

    @CheckReturnValue
    public RestAction<Message> send(String msg) {
        msg = truncate(StringUtils.replace(StringUtils.replace(msg, "@everyone", "@\u200beveryone")
                , "@here", "@\u200bhere")); // fully inline filter here for performance

        return channel.sendMessage(msg);
    }

    @CheckReturnValue
    public RestAction<Message> rawSend(String msg) {
        return channel.sendMessage(msg);
    }

    @CheckReturnValue
    public RestAction<Message> send(MessageEmbed msg) {
        return channel.sendMessage(msg);
    }

    @CheckReturnValue
    public RestAction<Message> send(Message msg) {
        return channel.sendMessage(msg);
    }

    public void fail(String msg) {
        send(Emotes.getFailure() + ' ' + msg).queue();
    }

    public void success(String msg) {
        send(Emotes.getSuccess() + ' ' + msg).queue();
    }
}
