package com.kdrag0n.bluestone.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class StringMapper {
    private final Map<String, Supplier<String>> resultMap = new HashMap<>();
    private Function<String, String> defaultCase;

    public String exec(String in) {
        if (resultMap.containsKey(in))
            return resultMap.get(in).get();
        else {
            if (defaultCase == null)
                return null;
            else
                return defaultCase.apply(in);
        }
    }

    public StringMapper map(String in, Supplier<String> supplier) {
        resultMap.put(in, supplier);
        return this;
    }

    public StringMapper map(String in, String out) {
        resultMap.put(in, () -> out);
        return this;
    }

    public StringMapper orElse(Function<String, String> func) {
        defaultCase = func;
        return this;
    }

    public static StringMapper match() {
        return new StringMapper();
    }
}