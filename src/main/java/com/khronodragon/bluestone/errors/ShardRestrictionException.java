package com.khronodragon.bluestone.errors;

class ShardRestrictionException extends IllegalStateException {
    public ShardRestrictionException() {}

    public ShardRestrictionException(String message) {
        super(message);
    }

    public ShardRestrictionException(Throwable cause) {
        super(cause);
    }

    public ShardRestrictionException(String message, Throwable cause) {
        super(message, cause);
    }
}
