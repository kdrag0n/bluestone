package com.khronodragon.bluestone.errors;

import net.dv8tion.jda.core.Permission;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PermissionError extends CheckFailure {
    private String[] erroredPerms;

    public PermissionError() {}

    public PermissionError(String message) {
        super(message);
    }

    public PermissionError(Throwable cause) {
        super(cause);
    }

    public PermissionError(String message, Throwable cause) {
        super(message, cause);
    }

    public PermissionError(String message, Throwable cause,
                        boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public PermissionError setPerms(String[] perms) {
        erroredPerms = perms;
        return this;
    }

    public String[] getFriendlyPerms() {
        return Arrays.stream(erroredPerms).map(p -> {
            if (p.equals("owner")) {
                return "Bot Owner";
            } else if (p.equals("admin")) {
                return "Bot Admin";
            } else {
                String jdaPermStr = String.join("_",
                        Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(p))
                                .map(String::toUpperCase)
                                .collect(Collectors.toList()));
                Permission perm = Permission.valueOf(jdaPermStr);
                if (perm == null) {
                    return "Unknown";
                } else {
                    return perm.getName();
                }
            }
        }).collect(Collectors.toList()).toArray(new String[0]);
    }
}
