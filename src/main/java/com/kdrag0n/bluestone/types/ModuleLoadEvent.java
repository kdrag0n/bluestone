package com.kdrag0n.bluestone.types;

import com.kdrag0n.bluestone.Bot;
import net.dv8tion.jda.api.events.Event;

public class ModuleLoadEvent extends Event {
    private final Bot bot;

    public ModuleLoadEvent(Bot bot) {
        super(null, 0);
        this.bot = bot;
    }
}
