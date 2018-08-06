package com.kdrag0n.bluestone.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class RandomSelect<T> {
    private final List<Supplier<T>> funcs;

    /**
     * Construct a new {@link RandomSelect} instance with 16 function slots pre-allocated.
     */
    private RandomSelect() {
        this(16);
    }

    /**
     * Construct a new {@link RandomSelect} instance with {@literal numFunctions} function slots pre-allocated.
     * @param numFunctions number of functions to expect
     */
    public RandomSelect(int numFunctions) {
        funcs = new ArrayList<>(numFunctions);
    }

    /**
     * Construct a new {@link RandomSelect} instance with 16 function slots pre-allocated.
     */
    public static<T> RandomSelect<T> create() {
        return new RandomSelect<>();
    }

    /**
     * Construct a new {@link RandomSelect} instance with {@literal numFunctions} function slots pre-allocated.
     * @param numFunctions number of functions to expect
     */
    public static<T> RandomSelect<T> create(int numFunctions) {
        return new RandomSelect<>(numFunctions);
    }

    /**
     * Add a function to the selection options.
     * @param func the function to add
     * @return this instance of {@link RandomSelect}
     */
    public RandomSelect<T> add(Supplier<T> func) {
        funcs.add(func);
        return this;
    }

    /**
     * Add an object to the selection options.
     * @param value the object to add
     * @return this instance of {@link RandomSelect}
     */
    public RandomSelect<T> add(T value) {
        funcs.add(() -> value);
        return this;
    }

    /**
     * Select a random options from the ones registered.
     * @return the option selected
     */
    public T select() {
        if (funcs.size() == 0) {
            throw new IllegalStateException("You must add at least 1 option before selection!");
        }

        return funcs.get(ThreadLocalRandom.current().nextInt(0, funcs.size())).get();
    }
}
