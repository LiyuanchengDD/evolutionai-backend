package com.example.grpcdemo.controller.dto;

public class TrialVerifyResponse {

    private final String status;
    private final String sentAt;
    private final String expiresAt;

    public TrialVerifyResponse(String status, String sentAt, String expiresAt) {
        this.status = status;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public String getSentAt() {
        return sentAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
