package com.khronodragon.bluestone.errors;

import net.dv8tion.jda.core.Permission;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionError extends CheckFailure {
    private Permission[] erroredPerms;

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

    public PermissionError setPerms(Permission[] perms) {
        erroredPerms = perms;
        return this;
    }

    public List<String> getFriendlyPerms() {
        return Arrays.stream(erroredPerms).map(Permission::getName)
                .collect(Collectors.toList());
    }
}
