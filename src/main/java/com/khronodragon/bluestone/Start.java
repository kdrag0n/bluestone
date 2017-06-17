package com.khronodragon.bluestone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Start {
    public static void main(String[] args) throws IOException {
        String jsonCode = new String(Files.readAllBytes(Paths.get("config.json")));
        JsonObject config = new JsonParser().parse(jsonCode).getAsJsonObject();

        String token = config.get("token").getAsString();
        int shardCount = config.get("shardCount").getAsInt(); // 1
        String type = config.get("type").getAsString(); // "bot"

        AccountType accountType;
        if (type.equals("bot")) {
            accountType = AccountType.BOT;
        } else if (type.equals("user")) {
            accountType = AccountType.CLIENT;
        } else {
            System.out.println("Warning: unrecognized account type! Use either 'client' (user) or 'bot' (bot). Assuming bot.");
            accountType = AccountType.BOT;
        }

        Unirest.setDefaultHeader("User-Agent", Bot.USER_AGENT);

        try {
            Bot.start(token, shardCount, accountType, config);
        } catch (LoginException|RateLimitedException e) {
            e.printStackTrace();
        }
    }
}
