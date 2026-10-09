package com.traveleasy.bus.security;

public enum UserRole {
    ROLE_TRAVELLER,
    ROLE_OPERATOR,
    ROLE_ADMIN,
    ROLE_SYSTEM;

    public static UserRole fromString(String roleName) {
        if (roleName == null) {
            return ROLE_TRAVELLER;
        }
        String normalized = roleName.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        return ROLE_TRAVELLER;
    }
}
