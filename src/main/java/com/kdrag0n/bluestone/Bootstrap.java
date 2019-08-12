package com.kdrag0n.bluestone;

import com.kdrag0n.bluestone.handlers.SentryFilter;
import io.sentry.Sentry;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class Bootstrap {
    private static final String[] dirInit = {"data/profiles/bg", "data/http_cache"};

    public static void main(String[] args) throws IOException {
        String jsonCode = new String(Files.readAllBytes(Paths.get("config.json")));
        JSONObject config = new JSONObject(jsonCode);
        if (!config.has("keys"))
            config.put("keys", new JSONObject());

        // init Sentry as early as possible to catch errors
        String sentryDSN;
        if ((sentryDSN = config.getJSONObject("keys").optString("sentry", null)) != null) {
            System.setProperty("sentry.release", BuildConfig.GIT_COMMIT);
            System.setProperty("sentry.tags", "jvm:" + System.getProperty("java.vm.name"));
            Sentry.init(sentryDSN);
        } else {
            SentryFilter.denyAll = true;
        }

        String token = config.getString("token");
        int shardCount = config.optInt("shard_count", 1); // 1

        for (String path: dirInit) {
            new File(path).mkdirs();
        }

        Bot.start(token, shardCount, config);
    }
}
