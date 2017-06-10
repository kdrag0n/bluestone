package com.khronodragon.bluestone.util;

import com.khronodragon.bluestone.Context;
import net.dv8tion.jda.core.entities.User;

import java.util.HashMap;
import java.util.Map;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;

public class TextParser {
    public static User parseUser(Context ctx) {
        String text = ctx.message.getRawContent();

        if (text.length() < 1) {
            return ctx.author;
        }

        Map<String, User> mentions = new HashMap<>();
        Map<Long, User> ids = new HashMap<>();
        Map<String, User> username = new HashMap<>();
        return ctx.author;
    }
}
