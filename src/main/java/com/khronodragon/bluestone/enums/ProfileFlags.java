package com.khronodragon.bluestone.enums;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Context;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import net.dv8tion.jda.core.entities.User;
import org.json.JSONArray;
import org.json.JSONException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileFlags {
    public static final int BOT_OWNER = 0x1;
    public static final int BOT_ADMIN = 0x2;
    public static final int PATREON_SUPPORTER = 0x4;

    public static int calculate(Context ctx, User user) {
        int flags = 0;

        if (user.getIdLong() == ctx.bot.getOwner().getIdLong())
            flags |= BOT_OWNER;

        try {
            if (ctx.bot.getAdminDao().idExists(user.getIdLong()))
                flags |= BOT_ADMIN;
        } catch (SQLException ignored) {}

        try {
            JSONArray patreonIds = Bot.patreonData.getJSONArray("user_ids");
            for (int i = 0; i < patreonIds.length(); i++) {
                if (patreonIds.getLong(i) == user.getIdLong()) {
                    flags |= PATREON_SUPPORTER;
                    break;
                }
            }
        } catch (JSONException ignored) {}

        return flags;
    }

    public static TIntList getFlags(Bot bot, User user) {
        TIntList flags = new TIntArrayList(3);

        if (user.getIdLong() == bot.getOwner().getIdLong())
            flags.add(BOT_OWNER);

        try {
            if (bot.getAdminDao().idExists(user.getIdLong()))
                flags.add(BOT_ADMIN);
        } catch (SQLException ignored) {}

        try {
            JSONArray patreonIds = Bot.patreonData.getJSONArray("user_ids");
            for (int i = 0; i < patreonIds.length(); i++) {
                if (patreonIds.getLong(i) == user.getIdLong()) {
                    flags.add(PATREON_SUPPORTER);
                    break;
                }
            }
        } catch (JSONException ignored) {}

        return flags;
    }
}
