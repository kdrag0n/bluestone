package com.khronodragon.bluestone;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.function.Predicate;

public class MessageWaitEventListener extends ListenerAdapter {
    private ContainerCell<Message> lock;
    private Predicate<Message> check;

    public MessageWaitEventListener(ContainerCell<Message> lock, Predicate<Message> check) {
        this.lock = lock;
        this.check = check;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (check.test(event.getMessage())) {
            synchronized (lock) {
                lock.setValue(event.getMessage());
                lock.notify();
            }
        }
    }
}
