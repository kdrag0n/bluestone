package com.khronodragon.bluestone.emotes;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class BetterTTVEmoteProvider implements EmoteProvider {
    private JSONObject emotes = null;
    private String template = null;

    public BetterTTVEmoteProvider(OkHttpClient client) {
        client.newCall(new Request.Builder()
                .get()
                .url("https://api.betterttv.net/2/emotes")
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogManager.getLogger(BetterTTVEmoteProvider.class).error("Failed to get data", e);

                emotes = new JSONObject();
                template = "";
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject data = new JSONObject(response.body().string());
                JSONArray rawEmotes = data.getJSONArray("emotes");
                JSONObject tempEmotes = new JSONObject();

                for (Object emote: rawEmotes) {
                    JSONObject realEmote = (JSONObject) emote;
                    final String name = realEmote.getString("code");
                    realEmote.remove("code");
                    realEmote.remove("restrictions");
                    realEmote.remove("channel");

                    tempEmotes.put(name, realEmote);
                }
                emotes = tempEmotes;
                template = "https:" + StringUtils.replaceOnce(data.getString("urlTemplate"), "{{image}}", "2x");

                LogManager.getLogger(BetterTTVEmoteProvider.class).info("Data loaded.");
            }
        });
    }

    @Override
    public boolean hasEmote(String emote) {
        return emotes.has(emote);
    }

    @Override
    public boolean isLoaded() {
        return emotes != null && template != null;
    }

    @Override
    public String getUrl(String emote) {
        if (emotes.has(emote)) {
            return StringUtils.replaceOnce(template, "{{id}}", emotes.getJSONObject(emote).getString("id"));
        } else {
            return null;
        }
    }

    @Override
    public EmoteInfo getEmoteInfo(String emote) {
        if (emotes.has(emote)) {
            JSONObject emoteObj = emotes.getJSONObject(emote);
            return new EmoteInfo(emote, emoteObj.getString("id"), null);
        } else {
            return null;
        }
    }
}
