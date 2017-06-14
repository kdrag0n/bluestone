package com.khronodragon.bluestone.emotes;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.khronodragon.bluestone.util.Strings.str;

public class FrankerFaceZEmoteProvider implements EmoteProvider {
    private JSONObject emotes = null;

    public FrankerFaceZEmoteProvider() {
        Unirest.get("https://api.frankerfacez.com/v1/emoticons?sort=count-desc&per_page=200&page=1")
                .asJsonAsync(new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> response) {
                        JSONObject data = response.getBody().getObject();
                        JSONArray rawEmotes = data.getJSONArray("emoticons");
                        JSONObject tempEmotes = new JSONObject();

                        for (Object emote: rawEmotes) {
                            JSONObject realEmote = (JSONObject) emote;
                            final String name = realEmote.getString("name");
                            realEmote.remove("name");
                            realEmote.remove("css");
                            realEmote.remove("margins");
                            realEmote.remove("public");
                            realEmote.remove("hidden");
                            realEmote.remove("modifier");
                            realEmote.remove("offset");

                            tempEmotes.put(name, realEmote);
                        }
                        emotes = tempEmotes;
                        LogManager.getLogger(FrankerFaceZEmoteProvider.class).info("Data loaded.");
                    }

                    @Override
                    public void failed(UnirestException e) {
                        LogManager.getLogger(FrankerFaceZEmoteProvider.class).error("Failed to get data", e);
                    }

                    @Override
                    public void cancelled() {
                        LogManager.getLogger(FrankerFaceZEmoteProvider.class).error("Data request cancelled!");
                    }
                });
    }

    @Override
    public boolean hasEmote(String emote) {
        return emotes.has(emote);
    }

    @Override
    public boolean isLoaded() {
        return emotes != null;
    }

    @Override
    public String getUrl(String emote) {
        if (emotes.has(emote)) {
            return "https:" + emotes.getJSONObject(emote).getJSONObject("urls").getString("2");
        } else {
            return null;
        }
    }

    @Override
    public EmoteInfo getEmoteInfo(String emote) {
        if (emotes.has(emote)) {
            JSONObject emoteObj = emotes.getJSONObject(emote);
            return new EmoteInfo(emote, str(emoteObj.getInt("id")), null);
        } else {
            return null;
        }
    }
}
