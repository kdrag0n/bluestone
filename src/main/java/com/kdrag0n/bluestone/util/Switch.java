package com.kdrag0n.bluestone.util;

import sun.misc.Unsafe;

/**
 * A more flexible switch-case system.
 * @param <T> Type of object to switch with
 */
public class Switch<T> {
    private static final Unsafe unsafe = com.kdrag0n.bluestone.Bot.getUnsafe();
    private final T obj;
    private boolean mode = true; // false = Object#equals(), true = identity (==)

    /**
     * Constructs a new instance of Switch, configured to switch with {@literal object}.
     * @param object object to switch with
     */
    public Switch(T object) {
        obj = object;
    }

    /**
     *  Constructs a new instance of Switch, configured to switch with {@literal object}.
     * @param object object to switch with
     * @param <A> type of object to switch with
     * @return the new instance of Switch
     */
    public static<A> Switch<A> with(A object) {
        return new Switch<>(object);
    }

    /**
     * Match objects by exact identity/instance, not by the data it contains.
     * @return this instance of Switch
     */
    public Switch<T> byInstance() {
        mode = true;
        return this;
    }

    /**
     * Match objects by the data they contain, not by exact identity/instance.
     * @return this instance of Switch
     */
    public Switch<T> byData() {
        mode = false;
        return this;
    }

    /**
     * If the {@literal object} initially set when creating this instance of {@literal Switch} matches the provided
     * {@literal object}, execute {@literal func}. If they don't match, do nothing.
     * @param object object to test with
     * @param func the function to execute, if objects match
     * @return this instance of Switch
     * @throws Throwable any exception that {@literal func} throws
     */
    public Switch<T> match(T object, ThrowingRunnable func) {
        if (mode ? obj == object : (obj == null ? object == null : obj.equals(object))) { // messy, I know
            try {
                func.run();
            } catch (Throwable t) {
                unsafe.throwException(t);
            }
        }

        return this;
    }

    /**
     * As a fallback if the object doesn't match any defined cases, execute this function.
     * @param func the function to execute
     * @return this instance of Switch
     * @throws Throwable any exception that {@literal func} throws
     */
    public Switch<T> fallback(ThrowingRunnable func) {
        try {
            func.run();
        } catch (Throwable t) {
            unsafe.throwException(t);
        }
        return this;
    }

    /**
     * A variant of {@link Runnable} that allows the function to throw any {@link Throwable\}.
     */
    public interface ThrowingRunnable {
        /**
         * The function to run.
         * @throws Throwable any exception
         */
        void run() throws Throwable;
    }
}
