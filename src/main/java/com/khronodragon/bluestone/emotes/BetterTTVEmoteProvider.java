package com.khronodragon.bluestone.emotes;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class BetterTTVEmoteProvider implements EmoteProvider {
    private JSONObject emotes = null;
    private String template = null;

    public BetterTTVEmoteProvider() {
        Unirest.get("https://api.betterttv.net/2/emotes")
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {
                        JSONObject data = response.getBody().getObject();
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

                        template = StringUtils.replaceOnce(data.getString("urlTemplate"), "{{image}}", "2x");
                    }

                    @Override
                    public void failed(UnirestException e) {
                        LogManager.getLogger(BetterTTVEmoteProvider.class).error("Failed to get data", e);
                    }

                    @Override
                    public void cancelled() {
                        LogManager.getLogger(BetterTTVEmoteProvider.class).error("Data request cancelled");
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
