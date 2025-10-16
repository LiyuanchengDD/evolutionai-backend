package com.example.grpcdemo.controller.dto;

public class TrialSendResponse {

    private final String status;
    private final String sentAt;
    private final String expiresAt;
    private final String code;
    private final boolean sent;

    public TrialSendResponse(String status, String sentAt, String expiresAt, String code, boolean sent) {
        this.status = status;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
        this.code = code;
        this.sent = sent;
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

    public String getCode() {
        return code;
    }

    public boolean isSent() {
        return sent;
    }
}
