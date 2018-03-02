package com.khronodragon.bluestone.errors;

class MessageException extends RuntimeException {
    MessageException() {
        super("Command was ended due to an error.");
    }

    MessageException(String msg) {
        super(msg);
    }

    MessageException(Throwable cause) {
        super(cause);
    }

    MessageException(String msg, Throwable cause) {
        super(msg, cause);
    }

    MessageException(String msg, Throwable cause,
                     boolean enableSuppression, boolean writableStackTrace) {
        super(msg, cause, enableSuppression, writableStackTrace);
    }
}
