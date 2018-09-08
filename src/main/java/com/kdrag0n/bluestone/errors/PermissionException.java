package com.kdrag0n.bluestone.errors;

import com.kdrag0n.bluestone.Perm;
import net.dv8tion.jda.core.Permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionException extends RuntimeException {
    private final List<Perm> erroredPerms;
    private static final List<Perm> unknownErrPerms = new ArrayList<>(1);

    static {
        unknownErrPerms.add(Perm.UNKNOWN);
    }

    public PermissionException(String message) {
        super(message);
        erroredPerms = unknownErrPerms;
    }

    public PermissionException(String message, Perm perm) {
        super(message);
        erroredPerms = new ArrayList<>(1);
        erroredPerms.add(perm);
    }

    public PermissionException(String message, List<Perm> perms) {
        super(message);
        erroredPerms = perms;
    }

    public List<String> getFriendlyPerms() {
        return erroredPerms.stream()
                .map(perm -> perm.name)
                .collect(Collectors.toList());
    }
}
