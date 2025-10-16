package com.example.grpcdemo.controller.dto;

import java.time.Instant;

public class AuthMeResponse {

    private final String userId;
    private final String email;
    private final boolean dev;
    private final Trial trial;

    public AuthMeResponse(String userId, String email, boolean dev, Trial trial) {
        this.userId = userId;
        this.email = email;
        this.dev = dev;
        this.trial = trial;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isDev() {
        return dev;
    }

    public Trial getTrial() {
        return trial;
    }

    public record Trial(String status, Instant expiresAt) {
    }
}

