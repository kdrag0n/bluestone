package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;

public class CosmeticCog extends Cog {
    public CosmeticCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Cosmetic";
    }
    public String getDescription() {
        return "Some nice cosmetic stuff for me to have.";
    }
}
