package com.kdrag0n.bluestone.types;

import com.kdrag0n.bluestone.Bot;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import net.dv8tion.jda.core.entities.User;

public class ProfileFlags {
    public static final int BOT_OWNER = 0x1;

    public static TIntList getFlags(Bot bot, User user) {
        TIntList flags = new TIntArrayList(3);

        if (user.getIdLong() == bot.ownerId)
            flags.add(BOT_OWNER);

        return flags;
    }
}
