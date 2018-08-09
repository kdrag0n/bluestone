package com.kdrag0n.bluestone.cogs;

import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.Cog;
import com.kdrag0n.bluestone.Context;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.annotations.DoNotAutoload;

@DoNotAutoload
public class StoryCog extends Cog {
    public StoryCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Stories";
    }

    public String getDescription() {
        return "A description.";
    }

    @Command(name = "story", desc = "Manage this server's stories, and publish/view them.")
    public void cmdStory(Context ctx) {
        ctx.fail("WIP!");
    }
}
