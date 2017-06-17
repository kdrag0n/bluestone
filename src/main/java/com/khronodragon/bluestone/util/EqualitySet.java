package com.khronodragon.bluestone.util;

import javax.annotation.Nonnull;
import java.util.*;

public class EqualitySet<T>
        extends AbstractSet<T>
        implements Set<T> {
    private transient List<T> list = new LinkedList<>();

    public Iterator<T> iterator() {
        return list.iterator();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(Object obj) {
        return list.stream()
                .anyMatch(o -> o.equals(obj));
    }

    public boolean add(T obj) {
        if (!contains(obj)) {
            list.add(obj);
            return true;
        } else
            return false;
    }

    public boolean remove(Object obj) {
        if (contains(obj)) {
            list.remove(obj);
            return true;
        } else
            return false;
    }

    public void clear() {
        list.clear();
    }

    public T normalize(@Nonnull T obj) {
        return list.stream()
                .filter(o -> o.equals(obj))
                .findFirst()
                .orElse(null);
    }
}
