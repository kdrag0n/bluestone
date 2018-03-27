package com.kdragon.bluestone.errors;

public class GuildOnlyError extends RuntimeException {
    public GuildOnlyError(String message) {
        super(message);
    }
}
