package com.kdrag0n.bluestone.handlers;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class MessageWaitEventListener extends ListenerAdapter {
    private AtomicReference<Message> lock;
    private Predicate<Message> check;

    public MessageWaitEventListener(AtomicReference<Message> lock, Predicate<Message> check) {
        this.lock = lock;
        this.check = check;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (check.test(event.getMessage())) {
            synchronized (lock) {
                lock.set(event.getMessage());
                lock.notify();
            }
        }
    }
}
