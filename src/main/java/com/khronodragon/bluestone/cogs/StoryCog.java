package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.DoNotAutoload;

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
        ctx.send(Emotes.getFailure() + " WIP!").queue();
    }
}
