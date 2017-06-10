package com.khronodragon.bluestone.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class Strings {
    private static final int bmpThreshold = 1 << 16;
    private static final int offset = 1 << 10;
    private static final int high = 0xd800;
    private static final int low = 0xdc00;

    public static int[] spread(String str) {
        List<Integer> codePoints = new ArrayList<>();
        int i = 0;

        while (i < str.length()) {
            int first = Character.codePointAt(str, i);
            i++;
            if (high <= first && first < high + offset) {
                int second = Character.codePointAt(str, i);
                i++;
                if (low <= second && second < low + offset) {
                    codePoints.add((first - high) * offset + (second - low) + bmpThreshold);
                } else {
                    return null;
                }
            } else {
                codePoints.add(first);
            }
        }
        return ArrayUtils.toPrimitive(codePoints.toArray(new Integer[0]));
    }

    public static String smartJoin(String[] array) {
        array[array.length - 1] = "or " + array[array.length - 1];
        return String.join(", ", array);
    }

    public static String str(long value) {
        return Long.toString(value);
    }

    public static String str(int value) {
        return Integer.toString(value);
    }
}
