package com.khronodragon.bluestone.errors;

public class CheckFailure extends RuntimeException {
    CheckFailure() {}

    public CheckFailure(String message) {
        super(message);
    }

    CheckFailure(Throwable cause) {
        super(cause);
    }

    CheckFailure(String message, Throwable cause) {
        super(message, cause);
    }

    CheckFailure(String message, Throwable cause,
                 boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
