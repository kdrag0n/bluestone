package com.kdrag0n.bluestone.types;

public enum BucketType {
    USER((byte)0),
    CHANNEL((byte)1),
    GUILD((byte)2),
    GLOBAL((byte)3);

    private final byte type;
    BucketType(byte type) {
        this.type = type;
    }
}
