package com.khronodragon.bluestone.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import org.apache.commons.lang3.ArrayUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {
    private static final int bmpThreshold = 1 << 16;
    private static final int offset = 1 << 10;
    private static final int high = 0xd800;
    private static final int low = 0xdc00;

    public static int[] spread(String str) {
        TIntList codePoints = new TIntLinkedList();
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

        return codePoints.toArray();
    }

    public static String smartJoin(String[] array) {
        return smartJoin(array, "and");
    }

    public static String smartJoin(List<String> list) {
        return smartJoin(list, "and");
    }

    public static String smartJoin(String[] array, String sep) {
        if (array.length > 1)
            array[array.length - 1] = sep + ' ' + array[array.length - 1];

        return String.join(", ", array);
    }

    public static String smartJoin(List<String> list, String sep) {
        if (list.size() > 1)
            list.set(list.size() - 1, sep + ' ' + list.get(list.size() - 1));

        return String.join(", ", list);
    }

    public static String str(long value) {
        return Long.toString(value);
    }

    public static String str(int value) {
        return Integer.toString(value);
    }

    public static String str(short value) {
        return Short.toString(value);
    }

    public static String str(double value) {
        return Double.toString(value);
    }

    public static String simpleJoin(List<String> strings) {
        StringBuilder builder = new StringBuilder();

        for (String str: strings) {
            builder.append(str);
        }

        return builder.toString();
    }

    public static String replace(String input, Pattern regex, StringReplacerCallback callback) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = regex.matcher(input);
        while (matcher.find()) {
            String rep = callback.replace(matcher.group(1));

            if (rep == null)
                matcher.appendReplacement(result, "**[__unknown key__]**");
            else
                matcher.appendReplacement(result, Matcher.quoteReplacement(rep));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public static StringMapper createMap() {
        return StringMapper.match();
    }

    public static String buildQueryUrl(String base, String... args) {
        if (args.length < 1)
            return base;
        if (args.length % 2 != 0)
            throw new IllegalArgumentException("Query parameters must be key, value");

        try {
            StringBuilder builder = new StringBuilder(base)
                    .append('?');

            for (int i = 0; i < args.length - 1; i += 2) {
                if (args.length > 2)
                    builder.append('&');

                builder.append(args[i])
                        .append('=')
                        .append(URLEncoder.encode(args[i + 1], "UTF-8"));
            }

            return builder.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
