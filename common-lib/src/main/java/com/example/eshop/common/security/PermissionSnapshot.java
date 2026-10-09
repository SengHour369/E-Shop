package com.example.eshop.common.security;

import java.util.Set;

/** Current identity and grants resolved by auth-service, never by the model. */
public record PermissionSnapshot(
        long userId,
        boolean administrator,
        Set<String> permissions) {

    public boolean permits(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
