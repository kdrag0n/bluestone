package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;

public class NotesCog extends Cog {
    public NotesCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Notes";
    }

    public String getDescription() {
        return "A description.";
    }

    @Command(name = "temp", desc = "")
    public void cmdTemp(Context ctx) {

    }
}
