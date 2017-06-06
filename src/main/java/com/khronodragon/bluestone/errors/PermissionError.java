package com.khronodragon.bluestone.errors;

public class PermissionError extends CheckFailure {
    public PermissionError() {}

    public PermissionError(String message) {
        super(message);
    }

    public PermissionError(Throwable cause) {
        super(cause);
    }

    public PermissionError(String message, Throwable cause) {
        super(message, cause);
    }

    public PermissionError(String message, Throwable cause,
                        boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
