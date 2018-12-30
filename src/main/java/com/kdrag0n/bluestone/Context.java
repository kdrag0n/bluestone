package com.kdrag0n.bluestone;

import com.kdrag0n.bluestone.util.ArrayListView;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.CheckReturnValue;

public class Context {
    private static final String truncateSuffix = "**...too long**";
    private static final String codeTruncateSuffix = "```" + truncateSuffix;
    public Bot bot;
    public final MessageReceivedEvent event;
    public final Message message;
    public User author;
    public final Guild guild;
    public final MessageChannel channel;
    public final Member member;
    public JDA jda;
    public final String prefix;
    public final ArrayListView args;
    public String invoker;
    public final String rawArgs;
    public boolean flag = false;

    public Context(Bot bot, MessageReceivedEvent event, ArrayListView args,
                   String prefix, String invoker, String content, boolean processArgs) {
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
        this.rawArgs = content.substring((processArgs ? prefix.length() : 0) + invoker.length()).trim();
    }

    public static String truncate(String msg) {
        if (msg.length() > 2000) {
            msg = msg.substring(0, 2000);

            if (StringUtils.countMatches(msg, "```") % 2 == 1) {
                return msg.substring(0, msg.length() - codeTruncateSuffix.length()) + codeTruncateSuffix;
            }

            return msg.substring(0, msg.length() - truncateSuffix.length()) + truncateSuffix;
        }

        return msg;
    }

    @CheckReturnValue
    public static String filterMessage(String msg) {
        return StringUtils.replace(StringUtils.replace(msg, "@everyone", "@\u200beveryone", -1),
                "@here", "@\u200bhere", -1);
    }

    @CheckReturnValue
    public RestAction<Message> send(String msg) {
        msg = truncate(StringUtils.replace(StringUtils.replace(msg, "@everyone", "@\u200beveryone", -1),
                "@here", "@\u200bhere", -1)); // fully inline

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
