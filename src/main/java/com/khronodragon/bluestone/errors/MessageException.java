package com.khronodragon.bluestone.errors;

public class MessageException extends RuntimeException {
    public MessageException() {
        super("Command was ended due to an error.");
    }

    public MessageException(String msg) {
        super(msg);
    }

    public MessageException(Throwable cause) {
        super(cause);
    }

    public MessageException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MessageException(String msg, Throwable cause,
                        boolean enableSuppression, boolean writableStackTrace) {
        super(msg, cause, enableSuppression, writableStackTrace);
    }
}
