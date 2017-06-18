package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

import static java.text.MessageFormat.format;

public class WebCog extends Cog {
    private boolean active = true;

    public WebCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Web";
    }

    public String getDescription() {
        return "The great web dashboard cog.";
    }

    @Command(name = "webstatus", desc = "Check the status of the web server.")
    public void cmdWebStatus(Context ctx) {
        ctx.send(format("The server is currently **{0}**.", active ? "running" : "down")).queue();
    }
}
