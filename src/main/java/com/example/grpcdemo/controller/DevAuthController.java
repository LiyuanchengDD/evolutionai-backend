package com.example.grpcdemo.controller;

import com.example.grpcdemo.security.dev.DevOtpService;
import com.example.grpcdemo.security.dev.InvalidDevOtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev/auth")
@ConditionalOnProperty(prefix = "app.auth", name = "mode", havingValue = "dev-otp")
public class DevAuthController {

    private static final Logger log = LoggerFactory.getLogger(DevAuthController.class);

    private final DevOtpService devOtpService;

    public DevAuthController(DevOtpService devOtpService) {
        this.devOtpService = devOtpService;
    }

    @PostMapping("/send-otp")
    public Map<String, Object> sendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "");
        Map<String, Object> response = devOtpService.sendOtp(email);
        log.info("[dev-otp] send code {} to email {}", response.get("code"), email);
        return response;
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "");
        String code = payload.getOrDefault("code", "");
        var token = devOtpService.verifyOtp(email, code);
        return Map.of(
                "access_token", token.token(),
                "token_type", "bearer",
                "expires_in", token.expiresInSeconds()
        );
    }

    @ExceptionHandler(InvalidDevOtpException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOtp() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errorCode", "INVALID_OTP", "message", "Invalid code"));
    }
}

