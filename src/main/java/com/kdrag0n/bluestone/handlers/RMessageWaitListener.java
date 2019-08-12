package com.kdrag0n.bluestone.handlers;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class RMessageWaitListener extends ListenerAdapter {
    private AtomicReference<Message> lock;
    private Predicate<Message> check;
    private Predicate<MessageReactionAddEvent> rCheck;
    private long channelId;

    public RMessageWaitListener(AtomicReference<Message> lock, Predicate<Message> check,
                                Predicate<MessageReactionAddEvent> rCheck, long channelId) {
        this.lock = lock;
        this.check = check;
        this.rCheck = rCheck;
        this.channelId = channelId;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != channelId) {
            return;
        }

        if (check.test(event.getMessage())) {
            synchronized (lock) {
                lock.set(event.getMessage());
                lock.notify();
            }
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getChannel().getIdLong() != channelId) {
            return;
        }

        if (rCheck.test(event)) {
            event.getChannel().retrieveMessageById(event.getMessageIdLong()).queue(msg -> {
                if (check.test(msg)) {
                    synchronized (lock) {
                        lock.set(msg);
                        lock.notify();
                    }
                }
            });
        }
    }
}
