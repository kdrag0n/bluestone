package com.kdragon.bluestone.util;

import java.util.List;

/**
 * A simple primitive List implementation that provides a view of an array + 1.
 * Immutable. Does <b>NOT</b> implement the actual {@link List} interface.
 *
 * <b>Ignores the first element of the array!</b>
 */
public class ArrayListView {
    public final String[] array;
    public final int length;
    private final int realLength;
    public final boolean empty;

    public ArrayListView(String[] array) {
        this.array = array;
        this.length = array.length - 1;
        this.realLength = array.length;
        this.empty = array.length == 1;
    }

    public String get(int i) {
        if (i < length) return array[i + 1];
        else return null;
    }

    public boolean contains(String obj) {
        for (int i = 1; i < realLength; i++) {
            if (array[i].equals(obj)) return true;
        }
        return false;
    }

    public String join(char sep) {
        // fast paths
        switch (length) {
            case 1: return array[1];
            case 2: return array[1] + sep + array[2];
            case 3: return array[1] + sep + array[2] + sep + array[3];
            case 4: return array[1] + sep + array[2] + sep + array[3] + sep + array[4];
            case 5: return array[1] + sep + array[2] + sep + array[3] + sep + array[4] + sep + array[5];
            default:
                StringBuilder sb = new StringBuilder(32);
                for (int i = 1; i < array.length; i++)
                    sb.append(array[i]);
                return sb.toString();
        }
    }
}
