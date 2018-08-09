package com.kdragon.bluestone.util;

import java.util.function.Supplier;

public class NullValueWrapper<T> {
    private final T origVal;

    private NullValueWrapper(T value) {
        origVal = value;
    }

    /**
     * Return the original value, or the specified value if null.
     * @param defaultValue the default value
     * @return either the original, or this value
     */
    public T or(T defaultValue) {
        if (origVal == null) {
            return defaultValue;
        } else {
            return origVal;
        }
    }

    /**
     * Return the original value, or the value returned by the specified function if nill.
     * @param defaultValue the default value supplier
     * @return either the original, or this value
     */
    public T or(Supplier<T> defaultValue) {
        if (origVal == null) {
            return defaultValue.get();
        } else {
            return origVal;
        }
    }

    /**
     * Create a new {@link NullValueWrapper} with the specified value.
     * @param val the value to use
     * @param <VT> the type of the value
     * @return a new {@link NullValueWrapper} containing the value
     */
    public static<VT> NullValueWrapper<VT> val(VT val) {
        return new NullValueWrapper<>(val);
    }
}
