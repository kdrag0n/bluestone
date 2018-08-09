package com.kdrag0n.bluestone.util;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
    /**
     * Return a stream which uses the given {@link Iterator}
     * @param sourceIterator the iterator to use
     * @param <T> the element type
     * @return a stream sourcing from the iterator
     */
    public static<T> Stream<T> asStream(Iterator<T> sourceIterator) {
        return asStream(sourceIterator, false);
    }

    /**
     * Return a stream which uses the given {@link Iterator}
     * @param sourceIterator the iterator to use
     * @param parallel whether to create a parallel stream
     * @param <T> the element type
     * @return a stream sourcing from the iterator
     */
    private static<T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    /**
     * Return a stream which uses the given {@link Iterable}
     * @param source the iterable to use
     * @param <T> the element type
     * @return a stream sourcing from the iterable
     */
    public static<T> Stream<T> asStream(Iterable<T> source) {
        return asStream(source, false);
    }

    /**
     * Return a stream which uses the given {@link Iterable}
     * @param source the iterable to use
     * @param parallel whether to create a parallel stream
     * @param <T> the element type
     * @return a stream sourcing from the iterable
     */
    private static<T> Stream<T> asStream(Iterable<T> source, boolean parallel) {
        return StreamSupport.stream(source.spliterator(), parallel);
    }
}
