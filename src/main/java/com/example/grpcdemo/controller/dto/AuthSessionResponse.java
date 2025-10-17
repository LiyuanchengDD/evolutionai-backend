package com.example.grpcdemo.controller.dto;

public record AuthSessionResponse(
        String userId,
        String email,
        String role,
        String accessToken,
        String refreshToken
) {
}
