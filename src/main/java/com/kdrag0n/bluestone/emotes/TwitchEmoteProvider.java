package com.kdrag0n.bluestone.emotes;

import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.util.JSONUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import static com.kdrag0n.bluestone.util.Strings.str;

public class TwitchEmoteProvider implements EmoteProvider {
    private static final Logger logger = LogManager.getLogger(TwitchEmoteProvider.class);

    private JSONObject emotes = new JSONObject();

    public TwitchEmoteProvider(OkHttpClient client) {
        client.newCall(new Request.Builder()
                .get()
                .url("https://twitchemotes.com/api_cache/v3/global.json")
                .build()).enqueue(Bot.callback(response -> {
            JSONObject data;
            try {
                data = new JSONObject(response.body().string());
            } catch (JSONException ignored) {
                logger.error("Invalid main emote JSON");
                return;
            }

            JSONUtils.addAllTo(emotes, data);
        }, e -> logger.error("Failed to get main emotes", e)));

        client.newCall(new Request.Builder()
                .get()
                .url("https://twitchemotes.com/api_cache/v3/subscriber.json")
                .build()).enqueue(Bot.callback(response -> {
            JSONObject data;
            try {
                data = new JSONObject(response.body().string());
            } catch (JSONException ignored) {
                logger.error("Invalid subscriber emote JSON");
                return;
            }

            for (String key: data.keySet()) {
                JSONObject channel = data.getJSONObject(key);

                for (Object iter: channel.getJSONArray("emotes")) {
                    JSONObject obj = (JSONObject) iter;
                    emotes.put(obj.getString("code"), obj);
                }
            }
        }, e -> logger.error("Failed to get subscriber emotes", e)));
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
