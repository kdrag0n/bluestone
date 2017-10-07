package com.khronodragon.bluestone.emotes;

import com.khronodragon.bluestone.util.JSONUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import static com.khronodragon.bluestone.util.Strings.str;

public class TwitchEmoteProvider implements EmoteProvider {
    public JSONObject emotes = new JSONObject();

    public TwitchEmoteProvider(OkHttpClient client) {
        emotes = new JSONObject();
        client.newCall(new Request.Builder()
                .get()
                .url("https://twitchemotes.com/api_cache/v3/global.json")
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogManager.getLogger(TwitchEmoteProvider.class).error("Failed to get main emotes", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONUtils.addAllTo(emotes, new JSONObject(response.body().string()));
                LogManager.getLogger(TwitchEmoteProvider.class).info("Main emotes loaded.");
            }
        });

        client.newCall(new Request.Builder()
                .get()
                .url("https://twitchemotes.com/api_cache/v3/subscriber.json")
                .build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogManager.getLogger(TwitchEmoteProvider.class).error("Failed to get subscriber emotes", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject data = new JSONObject(response.body().string());

                for (String key: data.keySet()) {
                    JSONObject channel = data.getJSONObject(key);

                    for (Object iter: channel.getJSONArray("emotes")) {
                        JSONObject obj = (JSONObject) iter;
                        emotes.put(obj.getString("code"), obj);
                    }
                }
                
                LogManager.getLogger(TwitchEmoteProvider.class).info("Subscriber emotes loaded.");
            }
        });
    }

    @Override
    public boolean hasEmote(String emote) {
        return emotes.has(emote);
    }

    @Override
    public String getUrl(String emote) {
        if (emotes.has(emote)) {
            return "https://static-cdn.jtvnw.net/emoticons/v1/" +
                    emotes.getJSONObject(emote).getInt("id") + "/2.0";
        } else {
            return null;
        }
    }

    @Override
    public EmoteInfo getEmoteInfo(String emote) {
        if (emotes.has(emote)) {
            JSONObject emoteObj = emotes.getJSONObject(emote);
            return new EmoteInfo(emote, str(emoteObj.getInt("id")), emoteObj.optString("description"));
        } else {
            return null;
        }
    }
}
