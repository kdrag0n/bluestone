package com.khronodragon.bluestone.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MinecraftUtil {
    private static final Map<String, String> FORMAT_KEYS = new HashMap<String, String>() {{
        put("bold", "**");
        put("italic", "*");
        put("underlined", "__");
        put("strikethrough", "~~");
    }};

    public static String decodeJsonText(JsonObject data) {
        List<String> segments = new LinkedList<>();

        for (JsonElement elem: data.getAsJsonArray("extra")) {
            JsonObject element = elem.getAsJsonObject();

            String item = element.get("text").getAsString();
            for (String fkey: FORMAT_KEYS.keySet()) {
                boolean hasKey = false;
                try {
                    hasKey = element.getAsJsonPrimitive(fkey).getAsBoolean();
                } catch (Exception ignored) {}

                if (hasKey) {
                    String intKey = "{F" + fkey + '}';
                    item = intKey + item + intKey;
                }
            }

            segments.add(item);
        }

        StringBuilder textBuilder = new StringBuilder();
        for (String segment: segments) {
            textBuilder.append(segment);
        }
        String text = textBuilder.toString();

        for (Map.Entry<String, String> fmtPair: FORMAT_KEYS.entrySet()) {
            String intKey = "{F" + fmtPair.getKey() + '}';

            text = text.replaceAll("(?:" + Pattern.quote(intKey) + "){2,}", "")
                    .replace(intKey, fmtPair.getValue());
        }

        return text;
    }
}
