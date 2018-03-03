package com.khronodragon.bluestone.util;

import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;

public class StackUtil {
    private static StringBuilder addVagueElement(StringBuilder builder, StackTraceElement elem) {
        return builder.append("> ")
                .append(StringUtils.replaceOnce(StringUtils.replaceOnce(elem.getClassName(),
                        "java.base/java.util", "stdlib"),
                        "com.khronodragon.bluestone", "bot"))
                .append('.')
                .append(elem.getMethodName())
                .append(elem.isNativeMethod() ? "(native)" : "(" + elem.getLineNumber() + ")");
    }

    public static String vagueTrace(Throwable e) {
        StackTraceElement[] elements = e.getStackTrace();
        StackTraceElement[] limitedElems = {elements[0], elements[1]};
        StringBuilder stack = new StringBuilder(e.getClass().getSimpleName())
                .append(": ")
                .append(e.getMessage());

        for (StackTraceElement elem: limitedElems) {
            stack.append("\n\u2007\u2007");
            addVagueElement(stack, elem);
        }

        if (stack.indexOf("> bot.cogs") == -1) {
            for (StackTraceElement elem: elements) {
                if (elem.getClassName().startsWith("com.khronodragon.bluestone.cogs.")) {
                    stack.append("\n\n\u2007\u2007");
                    addVagueElement(stack, elem);
                    break;
                }
            }
        }

        return stack.toString();
    }

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

    public static String briefSqlError(SQLException e) {
        String result = e.getMessage();
        if (e.getCause() != null) {
            result = result + "\n\u2007\u2007> " + e.getCause().getMessage();
        }

        return result;
    }
}
