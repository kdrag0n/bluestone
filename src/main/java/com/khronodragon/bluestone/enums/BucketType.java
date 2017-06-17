package com.khronodragon.bluestone.enums;

public enum BucketType {
    USER((short)0),
    CHANNEL((short)1),
    GUILD((short)2),
    GLOBAL((short)3);

    private final short type;
    BucketType(short type) {
        this.type = type;
    }
}
