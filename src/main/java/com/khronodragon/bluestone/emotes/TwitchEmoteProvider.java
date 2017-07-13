package com.khronodragon.bluestone.emotes;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import java.io.IOException;

import static com.khronodragon.bluestone.util.Strings.str;

public class TwitchEmoteProvider implements EmoteProvider {
    public JSONObject emotes = null;
    public JSONObject templates = null;

    public TwitchEmoteProvider(OkHttpClient client) {
        client.newCall(new Request.Builder()
                .get()
                .url("https://twitchemotes.com/api_cache/v2/global.json")
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogManager.getLogger(TwitchEmoteProvider.class).error("Failed to get data", e);

                emotes = new JSONObject();
                templates = new JSONObject();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject data = new JSONObject(response.body().string());

                emotes = data.getJSONObject("emotes");
                templates = data.getJSONObject("template");
                LogManager.getLogger(TwitchEmoteProvider.class).info("Data loaded.");
            }
        });
    }

    @Override
    public boolean hasEmote(String emote) {
        return emotes.has(emote);
    }

    @Override
    public boolean isLoaded() {
        return emotes != null && templates != null;
    }

    @Override
    public String getUrl(String emote) {
        if (emotes.has(emote)) {
            return StringUtils.replaceOnce(templates.getString("medium"), "{image_id}", str(emotes.getJSONObject(emote).getInt("image_id")));
        } else {
            return null;
        }
    }

    @Override
    public EmoteInfo getEmoteInfo(String emote) {
        if (emotes.has(emote)) {
            JSONObject emoteObj = emotes.getJSONObject(emote);
            return new EmoteInfo(emote, str(emoteObj.getInt("image_id")), emoteObj.optString("description"));
        } else {
            return null;
        }
    }
}
