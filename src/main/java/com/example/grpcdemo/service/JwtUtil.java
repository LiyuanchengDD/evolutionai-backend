package com.example.grpcdemo.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.util.Date;

/**
 * Utility for generating and validating JSON Web Tokens.
 */
public final class JwtUtil {

    private static final String SECRET_KEY = "secret-key";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);
    private static final JWTVerifier VERIFIER = JWT.require(ALGORITHM).build();

    private JwtUtil() {
    }

    public static String generateToken(String userId, String email, String role) {
        return JWT.create()
                .withSubject(userId)
                .withClaim("email", email)
                .withClaim("role", role)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(ALGORITHM);
    }

    public static DecodedJWT validateToken(String token) {
        return VERIFIER.verify(token);
    }
}

