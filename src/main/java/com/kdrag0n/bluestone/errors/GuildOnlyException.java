package com.kdrag0n.bluestone.errors;

public class GuildOnlyException extends RuntimeException {
    public GuildOnlyException(String message) {
        super(message);
    }
}
