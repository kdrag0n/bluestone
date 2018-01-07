package com.khronodragon.bluestone;

import io.sentry.Sentry;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Start {
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
        if ("bot".equals(type)) {
            accountType = AccountType.BOT;
        } else if ("user".equals(type)) {
            accountType = AccountType.CLIENT;
        } else {
            System.out.println("Warning: unrecognized account type! Use either 'client' (user) or 'bot' (bot). Assuming bot.");
            accountType = AccountType.BOT;
        }

        for (String path: dirInit) {
            new File(path).mkdirs();
        }

        try {
            Bot.start(token, shardCount, accountType, config);
        } catch (LoginException|RateLimitedException e) {
            e.printStackTrace();
        }
    }
}
