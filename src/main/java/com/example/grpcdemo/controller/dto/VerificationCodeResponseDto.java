package com.example.grpcdemo.controller.dto;

public class VerificationCodeResponseDto {

    private String requestId;
    private int expiresInSeconds;
    private String verificationCode;

    public VerificationCodeResponseDto(String requestId, int expiresInSeconds, String verificationCode) {
        this.requestId = requestId;
        this.expiresInSeconds = expiresInSeconds;
        this.verificationCode = verificationCode;
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

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}
