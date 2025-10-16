package com.example.grpcdemo.security.dev;

import com.example.grpcdemo.security.AppAuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DevOtpService {

    private static final String DEV_ISSUER = "app://dev";

    private final AppAuthProperties properties;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public DevOtpService(AppAuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        byte[] secret = properties.getDevOtp().getJwtSecret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    public Map<String, Object> sendOtp(String email) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("dev", true);
        payload.put("code", properties.getDevOtp().getCode());
        return payload;
    }

    public DevToken verifyOtp(String email, String code) {
        if (!properties.getDevOtp().getCode().equals(code)) {
            throw new InvalidDevOtpException();
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.getDevOtp().getTokenTtl());
        String userId = properties.getDevOtp().getUserId();
        if (userId == null || userId.isBlank()) {
            userId = UUID.randomUUID().toString();
        }
        String emailClaim = properties.getDevOtp().getUserEmail();
        if (emailClaim == null || emailClaim.isBlank()) {
            emailClaim = email;
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(DEV_ISSUER)
                .subject(userId)
                .claim("email", emailClaim)
                .claim("role", "authenticated")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        long expiresIn = properties.getDevOtp().getTokenTtl().toSeconds();
        return new DevToken(token, expiresIn);
    }

    public record DevToken(String token, long expiresInSeconds) {}
}

