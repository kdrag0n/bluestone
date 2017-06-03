package com.khronodragon.bluestone;

public interface ClassUtilities {
    default void print(String text) {
        System.out.println(text);
    }

    default void printf(String fmt, Object... args) {
        System.out.printf(fmt, args);
    }
}
