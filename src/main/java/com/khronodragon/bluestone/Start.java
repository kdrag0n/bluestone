package com.khronodragon.bluestone;

import io.sentry.Sentry;
import net.dv8tion.jda.core.AccountType;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class Start {
    private static final String[] dirInit = {"data/profiles/bg"};

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String jsonCode = new String(Files.readAllBytes(Paths.get("config.json")));
        JSONObject config = new JSONObject(jsonCode);

        // init Sentry as early as possible to catch errors
        String sentryDSN;
        if ((sentryDSN = config.getJSONObject("keys").optString("sentry", null)) != null) {
            Sentry.init(sentryDSN);
        }

        String token = config.getString("token");
        int shardCount = config.optInt("shard_count", 1); // 1
        String type = config.optString("type"); // "bot"

        AccountType accountType;
        switch (type) {
            case "bot":
                accountType = AccountType.BOT;
                break;
            case "user":
                accountType = AccountType.CLIENT;
                break;
            default:
                System.out.println("Warning: unrecognized account type! Use either 'client' (user) or 'bot' (bot). Assuming bot.");
                accountType = AccountType.BOT;
                break;
        }

        for (String path: dirInit) {
            new File(path).mkdirs();
        }

        Bot.start(token, shardCount, accountType, config);
    }
}
