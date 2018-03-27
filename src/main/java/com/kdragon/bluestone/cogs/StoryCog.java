package com.kdragon.bluestone.cogs;

import com.kdragon.bluestone.Bot;
import com.kdragon.bluestone.Cog;
import com.kdragon.bluestone.Context;
import com.kdragon.bluestone.annotations.Command;
import com.kdragon.bluestone.annotations.DoNotAutoload;

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
