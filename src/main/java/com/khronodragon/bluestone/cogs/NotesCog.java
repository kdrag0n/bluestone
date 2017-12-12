package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.DoNotAutoload;
import com.khronodragon.bluestone.errors.PermissionError;

@DoNotAutoload
public class NotesCog extends Cog {
    private static final String NO_COMMAND = "🤔 **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `create/new [text]` - create a new note\n" +
            "    \u2022 `max [number]` - set the maximum amount of notes members can have\n" +
            "    \u2022 `lock` - lock notes, preventing them from being added\n" +
            "    \u2022 `unlock` - unlock notes\n" +
            "    \u2022 `random` - show a random note\n" +
            "    \u2022 `clear` - clear notes\n" +
            "    \u2022 `ban [@member]` - ban someone from creating notes\n" +
            "\n" +
            "Notes can be retrieved at any time, edited by their creator, and will be posted in a channel.\n" +
            "They are private to each server - no other server can access them.\n" +
            "The owner can have unlimited notes, while others may have a configurable amount.\n" +
            "Those with **Manage Server** can have 100. There cannot be more than 999 notes per server.";

    public NotesCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Notes";
    }

    public String getDescription() {
        return "A description.";
    }

    @Command(name = "notes", desc = "Manage notes in this server.", aliases = {"note", "n"})
    public void mainCmd(Context ctx) throws PermissionError {

    }
}
