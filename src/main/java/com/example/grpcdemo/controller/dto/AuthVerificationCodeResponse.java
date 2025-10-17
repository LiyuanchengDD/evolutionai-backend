package com.example.grpcdemo.controller.dto;

public record AuthVerificationCodeResponse(String requestId, int expiresInSeconds) {
}
