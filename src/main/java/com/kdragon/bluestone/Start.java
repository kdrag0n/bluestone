package com.kdragon.bluestone;

import io.sentry.Sentry;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class Start {
    private static final String[] dirInit = {"data/profiles/bg", "data/http_cache"};

    public static void main(String[] args) throws IOException {
        String jsonCode = new String(Files.readAllBytes(Paths.get("config.json")));
        JSONObject config = new JSONObject(jsonCode);
        if (!config.has("keys")) config.put("keys", new JSONObject());

        // init Sentry as early as possible to catch errors
        String sentryDSN;
        if ((sentryDSN = config.getJSONObject("keys").optString("sentry", null)) != null) {
            System.setProperty("sentry.release", BuildConfig.GIT_COMMIT);
            System.setProperty("sentry.tags", "jvm:" + System.getProperty("java.vm.name") +
                    ",heap:" + (Runtime.getRuntime().maxMemory() / 1000000) + "mb");
            Sentry.init(sentryDSN);
        }

        String token = config.getString("token");
        int shardCount = config.optInt("shard_count", 1); // 1

        for (String path: dirInit) {
            new File(path).mkdirs();
        }

        Bot.start(token, shardCount, config);
    }
}
