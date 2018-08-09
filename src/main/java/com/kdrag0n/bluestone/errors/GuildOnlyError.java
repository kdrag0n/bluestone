package com.kdrag0n.bluestone.errors;

public class GuildOnlyError extends RuntimeException {
    public GuildOnlyError(String message) {
        super(message);
    }
}
