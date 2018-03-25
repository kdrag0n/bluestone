package com.khronodragon.bluestone.errors;

public class GuildOnlyError extends RuntimeException {
    public GuildOnlyError() {}

    public GuildOnlyError(String message) {
        super(message);
    }

    public GuildOnlyError(Throwable cause) {
        super(cause);
    }

    public GuildOnlyError(String message, Throwable cause) {
        super(message, cause);
    }

    public GuildOnlyError(String message, Throwable cause,
                           boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
