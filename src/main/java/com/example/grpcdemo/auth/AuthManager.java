package com.example.grpcdemo.auth;

import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final UserAccountRepository userRepository;
    private final Map<String, VerificationCodeRecord> codeStore = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random;
    private final Clock clock;

    public AuthManager(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, Clock.systemUTC(), new SecureRandom());
    }

    AuthManager(UserAccountRepository userRepository, PasswordEncoder passwordEncoder, Clock clock, SecureRandom random) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.random = random;
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
        return new VerificationResult(record.requestId(), (int) CODE_TTL.getSeconds());
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

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS);
        }

        String userId = UUID.randomUUID().toString();
        String hashed = passwordEncoder.encode(password);
        UserAccountEntity entity = new UserAccountEntity(userId, normalizedEmail, normalizedEmail, hashed, role.name());
        userRepository.save(entity);
        log.info("Registered new user: email={}, role={}", normalizedEmail, role);
        return createSession(entity);
    }

    public AuthSession login(String email, String password, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        UserAccountEntity entity = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));
        AuthRole storedRole = resolveRole(entity.getRole());
        if (storedRole != role) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, entity.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        return createSession(entity);
    }

    private AuthSession createSession(UserAccountEntity user) {
        AuthRole role = resolveRole(user.getRole());
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        return new AuthSession(user.getUserId(), user.getEmail(), role, accessToken, refreshToken);
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

    private AuthRole resolveRole(String value) {
        if (value == null || value.isBlank()) {
            throw new AuthException(AuthErrorCode.INVALID_ROLE, "角色信息缺失");
        }
        try {
            return AuthRole.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new AuthException(AuthErrorCode.INVALID_ROLE, "不支持的角色: " + value);
        }
    }

    private String codeKey(String email, AuthRole role) {
        return email + "|" + role.name();
    }

    public record VerificationResult(String requestId, int expiresInSeconds) {}

    public record AuthSession(String userId, String email, AuthRole role, String accessToken, String refreshToken) {}

    private record VerificationCodeRecord(String code, Instant expireAt, Instant lastSentAt, boolean consumed, String requestId) {
        VerificationCodeRecord markConsumed() {
            return new VerificationCodeRecord(code, expireAt, lastSentAt, true, requestId);
        }
    }

}
