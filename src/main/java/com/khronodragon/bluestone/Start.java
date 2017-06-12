package com.khronodragon.bluestone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Start {
    public static void main(String[] args) throws IOException {
        String jsonCode = new String(Files.readAllBytes(Paths.get("auth.json")));
        JsonObject auth = new JsonParser().parse(jsonCode).getAsJsonObject();

        String token = auth.get("token").getAsString();
        int shardCount = auth.get("shardCount").getAsInt(); // 1
        String type = auth.get("type").getAsString(); // "bot"

        AccountType accountType;
        if (type.equals("bot")) {
            accountType = AccountType.BOT;
        } else if (type.equals("user")) {
            accountType = AccountType.CLIENT;
        } else {
            System.out.println("Warning: unrecognized account type! Use either 'client' (user) or 'bot' (bot). Assuming bot.");
            accountType = AccountType.BOT;
        }

        try {
            Bot.start(token, shardCount, accountType, auth);
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (RateLimitedException e) {
            e.printStackTrace();
        }
    }
}
