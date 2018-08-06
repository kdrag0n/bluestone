package com.kdrag0n.bluestone.cogs;

@com.kdrag0n.bluestone.annotations.DoNotAutoload
public class StoryCog extends com.kdrag0n.bluestone.Cog {
    public StoryCog(com.kdrag0n.bluestone.Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Stories";
    }

    public String getDescription() {
        return "A description.";
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "story", desc = "Manage this server's stories, and publish/view them.")
    public void cmdStory(com.kdrag0n.bluestone.Context ctx) {
        ctx.fail("WIP!");
    }
}
