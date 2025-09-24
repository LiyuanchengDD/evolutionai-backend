package com.example.grpcdemo.controller;

import com.example.grpcdemo.auth.AuthManager;
import com.example.grpcdemo.auth.AuthRole;
import com.example.grpcdemo.controller.dto.AuthResponseDto;
import com.example.grpcdemo.controller.dto.LoginRequest;
import com.example.grpcdemo.controller.dto.RegisterRequest;
import com.example.grpcdemo.controller.dto.SendCodeRequest;
import com.example.grpcdemo.controller.dto.VerificationCodeResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthManager authManager;

    public AuthController(AuthManager authManager) {
        this.authManager = authManager;
    }

    @PostMapping("/{segment}/auth/send-code")
    public VerificationCodeResponseDto sendCode(@PathVariable("segment") String segment,
                                                @Valid @RequestBody SendCodeRequest request) {
        AuthRole role = resolveRole(segment);
        AuthManager.VerificationResult result = authManager.requestVerificationCode(request.getEmail(), role);
        return new VerificationCodeResponseDto(result.requestId(), result.expiresInSeconds());
    }

    @PostMapping("/{segment}/auth/register")
    public AuthResponseDto register(@PathVariable("segment") String segment,
                                    @Valid @RequestBody RegisterRequest request) {
        AuthRole role = resolveRole(segment);
        AuthManager.AuthSession session = authManager.register(request.getEmail(), request.getPassword(), request.getVerificationCode(), role);
        return toDto(session);
    }

    @PostMapping("/{segment}/auth/login")
    public AuthResponseDto login(@PathVariable("segment") String segment,
                                 @Valid @RequestBody LoginRequest request) {
        AuthRole role = resolveRole(segment);
        AuthManager.AuthSession session = authManager.login(request.getEmail(), request.getPassword(), role);
        return toDto(session);
    }

    private AuthRole resolveRole(String segment) {
        return switch (segment.toLowerCase()) {
            case "b", "company" -> AuthRole.COMPANY;
            case "c", "engineer" -> AuthRole.ENGINEER;
            default -> throw new IllegalArgumentException("未知的角色入口: " + segment);
        };
    }

    private AuthResponseDto toDto(AuthManager.AuthSession session) {
        return new AuthResponseDto(
                session.userId(),
                session.email(),
                session.role().alias(),
                session.accessToken(),
                session.refreshToken()
        );
    }
}
