package com.example.grpcdemo.controller;

import com.example.grpcdemo.auth.AuthErrorCode;
import com.example.grpcdemo.auth.AuthException;
import com.example.grpcdemo.auth.AuthManager;
import com.example.grpcdemo.auth.AuthRole;
import com.example.grpcdemo.controller.dto.AuthLoginRequest;
import com.example.grpcdemo.controller.dto.AuthRegisterRequest;
import com.example.grpcdemo.controller.dto.AuthResetPasswordRequest;
import com.example.grpcdemo.controller.dto.AuthSessionResponse;
import com.example.grpcdemo.controller.dto.AuthVerificationCodeRequest;
import com.example.grpcdemo.controller.dto.AuthVerificationCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthManager authManager;

    public AuthController(AuthManager authManager) {
        this.authManager = authManager;
    }

    @PostMapping("/register/code")
    public AuthVerificationCodeResponse requestRegistrationCode(@Valid @RequestBody AuthVerificationCodeRequest request) {
        AuthRole role = resolveRole(request.role());
        AuthManager.VerificationResult result = authManager.requestRegistrationCode(request.email(), role);
        return new AuthVerificationCodeResponse(result.requestId(), result.expiresInSeconds());
    }

    @PostMapping("/password/code")
    public AuthVerificationCodeResponse requestPasswordResetCode(@Valid @RequestBody AuthVerificationCodeRequest request) {
        AuthRole role = resolveRole(request.role());
        AuthManager.VerificationResult result = authManager.requestPasswordResetCode(request.email(), role);
        return new AuthVerificationCodeResponse(result.requestId(), result.expiresInSeconds());
    }

    @PostMapping("/register")
    public AuthSessionResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        AuthRole role = resolveRole(request.role());
        AuthManager.AuthSession session = authManager.register(
                request.email(),
                request.password(),
                request.verificationCode(),
                role
        );
        return toResponse(session);
    }

    @PostMapping("/login")
    public AuthSessionResponse login(@Valid @RequestBody AuthLoginRequest request) {
        AuthRole role = resolveRole(request.role());
        AuthManager.AuthSession session = authManager.login(request.email(), request.password(), role);
        return toResponse(session);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody AuthResetPasswordRequest request) {
        AuthRole role = resolveRole(request.role());
        authManager.resetPassword(request.email(), request.verificationCode(), request.newPassword(), role);
        return ResponseEntity.noContent().build();
    }

    private AuthSessionResponse toResponse(AuthManager.AuthSession session) {
        return new AuthSessionResponse(
                session.userId(),
                session.email(),
                session.role().alias(),
                session.accessToken(),
                session.refreshToken()
        );
    }

    private AuthRole resolveRole(String rawRole) {
        try {
            return AuthRole.fromAlias(rawRole);
        } catch (IllegalArgumentException ex) {
            throw new AuthException(AuthErrorCode.INVALID_ROLE);
        }
    }
}
