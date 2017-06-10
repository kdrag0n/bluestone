package com.khronodragon.bluestone;

import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

public class Context {
    public final Bot bot;
    public final MessageReceivedEvent event;
    public final Message message;
    public final User author;
    public final Guild guild;
    public final MessageChannel channel;
    public final Member member;
    public final JDA jda;
    public final String prefix;
    public final List<String> args;
    public final String invoker;
    public final String rawArgs;
    public final String mention;

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
        this.rawArgs = StringUtils.strip(message.getRawContent().substring(Math.min(prefix.length() + invoker.length() + 1, prefix.length() + invoker.length())));
    }

    public RestAction<Message> send(String msg) {
        if (msg.length() > 2000) {
            msg = msg.substring(0, 2000);
            String truncateString = "**...too long**";
            if (StringUtils.countMatches(msg, "```") % 2 == 1) {
                truncateString = "```" + truncateString;
            }
            msg = msg.substring(0, msg.length() - truncateString.length()) + truncateString;
        }

        msg = msg.replace("@everyone", "@\u200beveryone")
                .replace("@here", "@\u200bhere");

        return channel.sendMessage(msg);
    }

    public RestAction<Message> rawSend(String msg) {
        return channel.sendMessage(msg);
    }

    public RestAction<Message> send(MessageEmbed msg) {
        return channel.sendMessage(msg);
    }

    public RestAction<Message> send(Message msg) {
        return channel.sendMessage(msg);
    }
}
