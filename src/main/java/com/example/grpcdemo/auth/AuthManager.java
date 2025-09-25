package com.example.grpcdemo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class AuthManager {

    private static final Logger log = LoggerFactory.getLogger(AuthManager.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final Map<String, UserAccount> userStore = new ConcurrentHashMap<>();
    private final Map<String, VerificationCodeRecord> codeStore = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random;
    private final Clock clock;

    public AuthManager() {
        this(new BCryptPasswordEncoder(), new SecureRandom(), Clock.systemUTC());
    }

    public AuthManager(PasswordEncoder passwordEncoder, SecureRandom random, Clock clock) {
        this.passwordEncoder = passwordEncoder;
        this.random = random;
        this.clock = clock;
    }

    public VerificationResult requestVerificationCode(String email, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        Instant now = clock.instant();
        String key = codeKey(normalizedEmail, role);

        VerificationCodeRecord existing = codeStore.get(key);
        if (existing != null && Duration.between(existing.lastSentAt(), now).compareTo(RESEND_INTERVAL) < 0) {
            throw new AuthException(AuthErrorCode.CODE_REQUEST_TOO_FREQUENT,
                    "请在" + remainingSeconds(existing, now) + "秒后再试");
        }

        String code = generateCode();
        Instant expireAt = now.plus(CODE_TTL);
        VerificationCodeRecord record = new VerificationCodeRecord(code, expireAt, now, false, UUID.randomUUID().toString());
        codeStore.put(key, record);

        log.info("Generated verification code for email={}, role={}, code={}", normalizedEmail, role, code);
        return new VerificationResult(record.requestId(), (int) CODE_TTL.getSeconds(), code);
    }

    public AuthSession register(String email, String password, String verificationCode, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        validatePassword(password);

        String codeKey = codeKey(normalizedEmail, role);
        VerificationCodeRecord record = codeStore.get(codeKey);
        if (record == null || record.consumed()) {
            throw new AuthException(AuthErrorCode.CODE_NOT_FOUND);
        }

        Instant now = clock.instant();
        if (now.isAfter(record.expireAt())) {
            codeStore.remove(codeKey);
            throw new AuthException(AuthErrorCode.CODE_EXPIRED);
        }

        if (!Objects.equals(record.code(), verificationCode)) {
            throw new AuthException(AuthErrorCode.CODE_MISMATCH);
        }

        codeStore.put(codeKey, record.markConsumed());

        String userKey = userKey(normalizedEmail, role);
        UserAccount user = userStore.compute(userKey, (key, existing) -> {
            if (existing != null) {
                throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS);
            }
            String userId = UUID.randomUUID().toString();
            String hashed = passwordEncoder.encode(password);
            return new UserAccount(userId, normalizedEmail, role, hashed, now);
        });

        return createSession(user);
    }

    public AuthSession login(String email, String password, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        String userKey = userKey(normalizedEmail, role);
        UserAccount user = userStore.get(userKey);
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        return createSession(user);
    }

    private AuthSession createSession(UserAccount user) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        return new AuthSession(user.userId(), user.email(), user.role(), accessToken, refreshToken);
    }

    private long remainingSeconds(VerificationCodeRecord record, Instant now) {
        long elapsed = Duration.between(record.lastSentAt(), now).getSeconds();
        long wait = RESEND_INTERVAL.getSeconds() - elapsed;
        return Math.max(wait, 0);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new AuthException(AuthErrorCode.INVALID_EMAIL);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new AuthException(AuthErrorCode.PASSWORD_TOO_WEAK, "密码至少需要6位");
        }
    }

    private String generateCode() {
        int code = random.nextInt(900000) + 100000;
        return Integer.toString(code);
    }

    private String codeKey(String email, AuthRole role) {
        return email + "|" + role.name();
    }

    private String userKey(String email, AuthRole role) {
        return email + "|" + role.name();
    }

    public record VerificationResult(String requestId, int expiresInSeconds, String verificationCode) {}

    public record AuthSession(String userId, String email, AuthRole role, String accessToken, String refreshToken) {}

    private record VerificationCodeRecord(String code, Instant expireAt, Instant lastSentAt, boolean consumed, String requestId) {
        VerificationCodeRecord markConsumed() {
            return new VerificationCodeRecord(code, expireAt, lastSentAt, true, requestId);
        }
    }

    private record UserAccount(String userId, String email, AuthRole role, String passwordHash, Instant createdAt) {}
}
