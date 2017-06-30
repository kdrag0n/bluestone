package com.khronodragon.bluestone.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class UnisafeString {
    private final String data;
    private final int length;

    public UnisafeString(String stringIn) {
        data = Objects.requireNonNull(stringIn);
        length = data.codePointCount(0, data.length());
    }

    public int length() {
        return length;
    }

    public int charAt(int index) {
        return data.codePointAt(getRealIndex(index));
    }

    public UnisafeString substring(int startIndex, int endIndex) {
        int cpStartIndex = getRealIndex(startIndex);
        int cpEndIndex = getRealIndex(endIndex);
        return new UnisafeString(data.substring(cpStartIndex, cpEndIndex));
    }

    public IntStream charStream() {
        return data.codePoints();
    }

    public PrimitiveIterator.OfInt chars() {
        class CodePointIterator implements PrimitiveIterator.OfInt {
            private int cur = 0;

            @Override
            public void forEachRemaining(IntConsumer block) {
                final int length = length();
                int i = cur;
                try {
                    while (i < length) {
                        char c1 = data.charAt(i++);
                        if (!Character.isHighSurrogate(c1) || i >= length) {
                            block.accept(c1);
                        } else {
                            char c2 = data.charAt(i);
                            if (Character.isLowSurrogate(c2)) {
                                i++;
                                block.accept(Character.toCodePoint(c1, c2));
                            } else {
                                block.accept(c1);
                            }
                        }
                    }
                } finally {
                    cur = i;
                }
            }

            public boolean hasNext() {
                return cur < data.length();
            }

            public int nextInt() {
                final int length = data.length();

                if (cur >= length) {
                    throw new NoSuchElementException();
                }
                char c1 = data.charAt(cur++);
                if (Character.isHighSurrogate(c1) && cur < length) {
                    char c2 = data.charAt(cur);
                    if (Character.isLowSurrogate(c2)) {
                        cur++;
                        return Character.toCodePoint(c1, c2);
                    }
                }
                return c1;
            }
        }

        return new CodePointIterator();
    }

    private int getRealIndex(int index) {
        return data.offsetByCodePoints(0, index);
    }

    public int indexOf(int codePoint) {
        int i = 0;

        PrimitiveIterator.OfInt iterator = chars();
        while (iterator.hasNext()) {
            i++;
            if (iterator.nextInt() == codePoint)
                return i;
        }

        return -1;
    }

    public boolean equals(Object object) {
        if (object instanceof UnisafeString)
            return ((UnisafeString) object).data.equals(data);
        else if (object instanceof String)
            return object.equals(data);
        else
            return false;
    }

    public int hashCode() {
        return data.hashCode();
    }

    public String toString() {
        return data;
    }
}