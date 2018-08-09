package com.kdragon.bluestone.util;

import org.json.JSONObject;

public class JSONUtils {
    public static void addAllTo(JSONObject one, JSONObject two) {
        for (String key: two.keySet()) {
            one.put(key, two.get(key));
        }
    }
}
