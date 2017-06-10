package com.khronodragon.bluestone.util;

public class NullValueWrapper<T> {
    private T origVal;
    NullValueWrapper(T value) {
        origVal = value;
    }

    public T or(T defaultValue) {
        if (origVal == null) {
            return defaultValue;
        } else {
            return origVal;
        }
    }

    public static<VT> NullValueWrapper<VT> val(VT val) {
        return new NullValueWrapper(val);
    }
}
