package com.example.grpcdemo.controller.dto;

public class AuthMeResponse {

    private final String userId;
    private final String kind;
    private final Trial trial;

    public AuthMeResponse(String userId, String kind, Trial trial) {
        this.userId = userId;
        this.kind = kind;
        this.trial = trial;
    }

    public String getUserId() {
        return userId;
    }

    public String getKind() {
        return kind;
    }

    public Trial getTrial() {
        return trial;
    }

    public record Trial(String status, String sentAt, String expiresAt) {
    }
}

