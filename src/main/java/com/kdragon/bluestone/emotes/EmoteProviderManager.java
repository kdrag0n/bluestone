package com.kdragon.bluestone.emotes;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class EmoteProviderManager {
    private final List<EmoteProvider> providers = new LinkedList<>();

    public boolean canProvideEmote(String emote) {
        for (EmoteProvider provider: providers) {
            if (provider.hasEmote(emote))
                return true;
        }
        return false;
    }

    public String getFirstUrl(String emote) {
        for (EmoteProvider provider: providers) {
            if (provider.hasEmote(emote)) {
                final String result = provider.getUrl(emote);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    public EmoteInfo getFirstInfo(String emote) {
        for (EmoteProvider provider: providers) {
            if (provider.hasEmote(emote)) {
                final EmoteInfo result = provider.getEmoteInfo(emote);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    public List<EmoteProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    public boolean addProvider(EmoteProvider provider) {
        return providers.add(provider);
    }

    public boolean removeProvider(EmoteProvider provider) {
        return providers.remove(provider);
    }
}
