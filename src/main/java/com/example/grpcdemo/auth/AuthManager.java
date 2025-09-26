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

    private final Map<String, VerificationCodeRecord> codeStore = new ConcurrentHashMap<>();
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random;
    private final Clock clock;

    public AuthManager(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, new SecureRandom(), Clock.systemUTC());
    }

    AuthManager(UserAccountRepository userRepository, PasswordEncoder passwordEncoder, SecureRandom random, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.random = random;
        this.clock = clock;
    }

    public VerificationResult requestRegistrationCode(String email, AuthRole role) {
        return requestVerificationCode(email, role, VerificationPurpose.REGISTER);
    }

    public VerificationResult requestPasswordResetCode(String email, AuthRole role) {
        return requestVerificationCode(email, role, VerificationPurpose.RESET_PASSWORD);
    }

    public VerificationResult requestVerificationCode(String email,
                                                      AuthRole role,
                                                      VerificationPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> requestVerificationCodeInternal(email, role, VerificationPurpose.REGISTER, false);
            case RESET_PASSWORD -> requestVerificationCodeInternal(email, role, VerificationPurpose.RESET_PASSWORD, true);
        };
    }

    public void resetPassword(String email, String verificationCode, String newPassword, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        validatePassword(newPassword);

        String codeKey = codeKey(normalizedEmail, role, VerificationPurpose.RESET_PASSWORD);
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

        UserAccountEntity user = requireExistingUser(normalizedEmail, role);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Reset password for email={}, role={}", normalizedEmail, role);
    }

    public AuthSession register(String email, String password, String verificationCode, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        validatePassword(password);

        String codeKey = codeKey(normalizedEmail, role, VerificationPurpose.REGISTER);
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

        if (userRepository.findByUsernameAndRole(normalizedEmail, role.alias()).isPresent()) {
            throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS);
        }

        String userId = UUID.randomUUID().toString();
        String hashed = passwordEncoder.encode(password);
        UserAccountEntity entity = new UserAccountEntity(userId, normalizedEmail, hashed, role.alias());
        userRepository.save(entity);

        return createSession(entity);
    }

    public AuthSession login(String email, String password, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        UserAccountEntity user = userRepository.findByUsernameAndRole(normalizedEmail, role.alias())
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        return createSession(user);
    }

    private VerificationResult requestVerificationCodeInternal(String email,
                                                               AuthRole role,
                                                               VerificationPurpose purpose,
                                                               boolean requireExistingUser) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        if (requireExistingUser) {
            requireExistingUser(normalizedEmail, role);
        } else if (purpose == VerificationPurpose.REGISTER
                && userRepository.findByUsernameAndRole(normalizedEmail, role.alias()).isPresent()) {
            throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS);
        }

        Instant now = clock.instant();
        String key = codeKey(normalizedEmail, role, purpose);

        VerificationCodeRecord existing = codeStore.get(key);
        if (existing != null && Duration.between(existing.lastSentAt(), now).compareTo(RESEND_INTERVAL) < 0) {
            throw new AuthException(AuthErrorCode.CODE_REQUEST_TOO_FREQUENT,
                    "请在" + remainingSeconds(existing, now) + "秒后再试");
        }

        String code = generateCode();
        Instant expireAt = now.plus(CODE_TTL);
        VerificationCodeRecord record = new VerificationCodeRecord(code, expireAt, now, false, UUID.randomUUID().toString());
        codeStore.put(key, record);

        log.info("Generated verification code for email={}, role={}, purpose={}, code={}",
                normalizedEmail, role, purpose, code);
        return new VerificationResult(record.requestId(), (int) CODE_TTL.getSeconds());
    }

    private AuthSession createSession(UserAccountEntity user) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        AuthRole role = AuthRole.fromAlias(user.getRole());
        return new AuthSession(user.getUserId(), user.getUsername(), role, accessToken, refreshToken);
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

    private String codeKey(String email, AuthRole role, VerificationPurpose purpose) {
        return email + "|" + role.name() + "|" + purpose.name();
    }

    private UserAccountEntity requireExistingUser(String email, AuthRole role) {
        return userRepository.findByUsernameAndRole(email, role.alias())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND, "该邮箱尚未注册"));
    }

    public record VerificationResult(String requestId, int expiresInSeconds) {}

    public record AuthSession(String userId, String email, AuthRole role, String accessToken, String refreshToken) {}

    private record VerificationCodeRecord(String code, Instant expireAt, Instant lastSentAt, boolean consumed, String requestId)
    {
        VerificationCodeRecord markConsumed() {
            return new VerificationCodeRecord(code, expireAt, lastSentAt, true, requestId);
        }
    }
}
