package com.khronodragon.bluestone.errors;

public class UserNotFound extends MessageException {
    public UserNotFound() {
        super("**User not found! Mention, name, nickname, tag, or ID will work.**");
    }

    public UserNotFound(String msg) {
        super(msg);
    }

    public UserNotFound(Throwable cause) {
        super(cause);
    }

    public UserNotFound(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserNotFound(String msg, Throwable cause,
                           boolean enableSuppression, boolean writableStackTrace) {
        super(msg, cause, enableSuppression, writableStackTrace);
    }
}
