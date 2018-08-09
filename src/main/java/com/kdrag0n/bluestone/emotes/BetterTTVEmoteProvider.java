package com.kdrag0n.bluestone.emotes;

import com.kdrag0n.bluestone.Bot;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class BetterTTVEmoteProvider implements EmoteProvider {
    private JSONObject emotes = new JSONObject();
    private String template = "";

    public BetterTTVEmoteProvider(OkHttpClient client) {
        client.newCall(new Request.Builder().get().url("https://api.betterttv.net/2/emotes").build())
                .enqueue(Bot.callback(response -> {
                    JSONObject data = new JSONObject(response.body().string());
                    JSONArray rawEmotes = data.getJSONArray("emotes");
                    JSONObject tempEmotes = new JSONObject();

                    for (Object emote : rawEmotes) {
                        JSONObject realEmote = (JSONObject) emote;
                        final String name = realEmote.getString("code");
                        realEmote.remove("code");
                        realEmote.remove("restrictions");
                        realEmote.remove("channel");

                        tempEmotes.put(name, realEmote);
                    }
                    emotes = tempEmotes;
                    template = "https:" + StringUtils.replaceOnce(data.getString("urlTemplate"), "{{image}}", "2x");
                }, e -> LoggerFactory.getLogger(BetterTTVEmoteProvider.class).error("Failed to get data", e)));
    }

    @Override
    public boolean hasEmote(String emote) {
        return emotes.has(emote);
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
