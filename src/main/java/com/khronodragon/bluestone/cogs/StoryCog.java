package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

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

    @Command(name = "temp", desc = "")
    public void cmdTemp(Context ctx) {

    }
}
