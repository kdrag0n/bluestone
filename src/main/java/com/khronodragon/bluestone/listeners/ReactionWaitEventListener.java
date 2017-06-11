package com.khronodragon.bluestone.listeners;

import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class ReactionWaitEventListener extends ListenerAdapter {
    private AtomicReference<MessageReactionAddEvent> lock;
    private Predicate<MessageReactionAddEvent> check;

    public ReactionWaitEventListener(AtomicReference<MessageReactionAddEvent> lock, Predicate<MessageReactionAddEvent> check) {
        this.lock = lock;
        this.check = check;
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (check.test(event)) {
            synchronized (lock) {
                lock.set(event);
                lock.notify();
            }
        }
    }
}
