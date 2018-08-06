package com.kdrag0n.bluestone.errors;

import net.dv8tion.jda.core.Permission;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionError extends RuntimeException {
    private Permission[] erroredPerms;

    public PermissionError() {}

    public PermissionError(String message) {
        super(message);
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
