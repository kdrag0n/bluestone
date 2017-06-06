package com.khronodragon.bluestone.errors;

public class CheckFailure extends RuntimeException {
    public CheckFailure() {}

    public CheckFailure(String message) {
        super(message);
    }

    public CheckFailure(Throwable cause) {
        super(cause);
    }

    public CheckFailure(String message, Throwable cause) {
        super(message, cause);
    }

    public CheckFailure(String message, Throwable cause,
                           boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
