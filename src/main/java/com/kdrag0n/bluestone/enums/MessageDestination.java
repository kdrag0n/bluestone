package com.kdrag0n.bluestone.enums;

import com.kdrag0n.bluestone.Context;
import net.dv8tion.jda.core.entities.MessageChannel;

public enum MessageDestination {
    CHANNEL((byte)1),
    AUTHOR((byte)2);

    private final byte type;
    MessageDestination(byte type) {
        this.type = type;
    }

    public MessageChannel getChannel(Context ctx) {
        if (type == (byte)1) {
            return ctx.channel;
        } else {
            return ctx.author.openPrivateChannel().complete();
        }
    }
}
