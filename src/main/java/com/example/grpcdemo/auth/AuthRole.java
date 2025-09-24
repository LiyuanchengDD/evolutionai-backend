package com.example.grpcdemo.auth;

import java.util.Locale;

public enum AuthRole {
    COMPANY("company", "ENTERPRISE"),
    ENGINEER("engineer", "ENGINEER");

    private final String alias;
    private final String grpcValue;

    AuthRole(String alias, String grpcValue) {
        this.alias = alias;
        this.grpcValue = grpcValue;
    }

    public String alias() {
        return alias;
    }

    public String grpcValue() {
        return grpcValue;
    }

    public static AuthRole fromAlias(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (AuthRole role : values()) {
            if (role.alias.equals(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported role alias: " + value);
    }

    public static AuthRole fromGrpcValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (AuthRole role : values()) {
            if (role.grpcValue.equals(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported role value: " + value);
    }
}
