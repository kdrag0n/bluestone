package com.khronodragon.bluestone;

import net.dv8tion.jda.core.entities.MessageChannel;

public enum MessageDestination {
    CHANNEL((short)1),
    AUTHOR((short)2);

    private final short type;
    MessageDestination(short type) {
        this.type = type;
    }

    public MessageChannel getChannel(Context ctx) {
        if (type == (short)1) {
            return ctx.channel;
        } else {
            return ctx.author.openPrivateChannel().complete();
        }
    }
}
