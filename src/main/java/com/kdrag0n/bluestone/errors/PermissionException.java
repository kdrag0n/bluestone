package com.kdrag0n.bluestone.errors;

import com.kdrag0n.bluestone.types.Perm;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionException extends RuntimeException {
    private static final EnumSet<Perm> UNKNOWN_ERR_PERMS = EnumSet.of(Perm.UNKNOWN);
    private final EnumSet<Perm> erroredPerms;

    public PermissionException(String message) {
        super(message);
        erroredPerms = UNKNOWN_ERR_PERMS;
    }

    public PermissionException(String message, Perm perm) {
        super(message);
        erroredPerms = EnumSet.of(perm);
    }

    public PermissionException(String message, EnumSet<Perm> perms) {
        super(message);
        erroredPerms = perms;
    }

    public List<String> getFriendlyPerms() {
        return erroredPerms.stream()
                .map(perm -> perm.name)
                .collect(Collectors.toList());
    }
}
