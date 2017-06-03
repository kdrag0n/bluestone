package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;

public class FunCog extends Cog {
    public FunCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Fun";
    }
    public String getDescription() {
        return "Who doesn't like fun?";
    }
}
