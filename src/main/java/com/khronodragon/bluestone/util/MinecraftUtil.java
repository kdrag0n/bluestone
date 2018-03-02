package com.khronodragon.bluestone.util;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MinecraftUtil {
    private static final Map<String, String> FORMAT_KEYS = new HashMap<>() {{
        put("bold", "**");
        put("italic", "*");
        put("underlined", "__");
        put("strikethrough", "~~");
    }};

    public static String decodeJsonText(JSONObject data) {
        List<String> segments = new LinkedList<>();

        for (Object elem: data.getJSONArray("extra")) {
            JSONObject element = (JSONObject) elem;

            StringBuilder item = new StringBuilder(element.optString("text", ""));
            for (String fkey: FORMAT_KEYS.keySet()) {
                if (element.optBoolean(fkey, false)) {
                    String intKey = "{F" + fkey + '}';
                    item.insert(0, intKey)
                            .append(intKey);
                }
            }

            segments.add(item.toString());
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
