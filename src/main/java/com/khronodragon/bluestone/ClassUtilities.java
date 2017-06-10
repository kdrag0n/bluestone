package com.khronodragon.bluestone;

import java.util.concurrent.ThreadLocalRandom;

public interface ClassUtilities {
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
