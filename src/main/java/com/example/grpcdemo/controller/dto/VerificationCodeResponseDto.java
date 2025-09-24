package com.example.grpcdemo.controller.dto;

public class VerificationCodeResponseDto {

    private String requestId;
    private int expiresInSeconds;

    public VerificationCodeResponseDto(String requestId, int expiresInSeconds) {
        this.requestId = requestId;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(int expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
