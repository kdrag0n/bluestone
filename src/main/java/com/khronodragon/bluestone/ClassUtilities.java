package com.khronodragon.bluestone;

import java.util.concurrent.ThreadLocalRandom;

public interface ClassUtilities {
    default void print(String text) {
        System.out.println(text);
    }

    default void printf(String fmt, Object... args) {
        System.out.printf(fmt + "\n", args);
    }

    default int randint(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    default boolean stringExists(String string, String... options) {
        for (String opt: options) {
            if (string.equals(opt)) {
                return true;
            }
        }
        return false;
    }
}
