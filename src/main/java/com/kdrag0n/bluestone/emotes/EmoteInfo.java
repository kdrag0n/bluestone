package com.kdrag0n.bluestone.emotes;

public class EmoteInfo {
    public final String name;
    public final String description;
    private final String id;

    EmoteInfo(String name, String id, String description) {
        this.name = name;
        this.id = id;
        this.description = description;
    }
}
