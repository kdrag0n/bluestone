package com.khronodragon.bluestone.emotes;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import static com.khronodragon.bluestone.util.Strings.str;

public class TwitchEmoteProvider implements EmoteProvider {
    public JSONObject emotes = null;
    public JSONObject templates = null;

    public TwitchEmoteProvider() {
        Unirest.get("https://twitchemotes.com/api_cache/v2/global.json")
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {
                        JSONObject data = response.getBody().getObject();
                        emotes = data.getJSONObject("emotes");
                        templates = data.getJSONObject("template");
                        LogManager.getLogger(TwitchEmoteProvider.class).info("Data loaded.");
                    }

                    @Override
                    public void failed(UnirestException e) {
                        LogManager.getLogger(TwitchEmoteProvider.class).error("Failed to get data", e);
                    }

                    @Override
                    public void cancelled() {
                        LogManager.getLogger(TwitchEmoteProvider.class).error("Data request cancelled!");
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
