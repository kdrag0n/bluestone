package com.kdrag0n.bluestone.util;

public class StackUtil {
    public static String renderStackTrace(Throwable e) {
        return renderStackTrace(e, "    ", "at ");
    }

    public static String renderStackTrace(Throwable e, String joinSpaces, String elemPrefix) {
        StackTraceElement[] elements = e.getStackTrace();
        StringBuilder stack = new StringBuilder(e.getClass().getSimpleName())
                .append(": ")
                .append(e.getMessage());

        for (StackTraceElement elem: elements) {
            stack.append('\n')
                    .append(joinSpaces)
                    .append(elemPrefix)
                    .append(elem);
        }

        return stack.toString();
    }
}
