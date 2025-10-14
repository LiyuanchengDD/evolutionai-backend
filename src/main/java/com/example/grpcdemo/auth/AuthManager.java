package com.example.grpcdemo.auth;

import com.example.grpcdemo.entity.AuthVerificationCodeEntity;
import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.entity.UserAccountStatus;
import com.example.grpcdemo.repository.AuthVerificationCodeRepository;
import com.example.grpcdemo.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuthManager {

    private static final Logger log = LoggerFactory.getLogger(AuthManager.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserAccountRepository userRepository;
    private final AuthVerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeSender verificationCodeSender;
    private final SecureRandom random;
    private final Clock clock;

    public AuthManager(UserAccountRepository userRepository,
                       AuthVerificationCodeRepository verificationCodeRepository,
                       PasswordEncoder passwordEncoder,
                       VerificationCodeSender verificationCodeSender,
                       SecureRandom random,
                       Clock clock) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationCodeSender = verificationCodeSender;
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

        Instant now = clock.instant();
        verificationCodeRepository.deleteByExpiresAtBefore(now);
        AuthVerificationCodeEntity record = verificationCodeRepository
                .findByEmailAndRoleAndPurpose(normalizedEmail, role.alias(), VerificationPurpose.RESET_PASSWORD)
                .orElseThrow(() -> new AuthException(AuthErrorCode.CODE_NOT_FOUND));
        if (record.isConsumed()) {
            throw new AuthException(AuthErrorCode.CODE_NOT_FOUND);
        }
        if (now.isAfter(record.getExpiresAt())) {
            verificationCodeRepository.deleteById(record.getCodeId());
            throw new AuthException(AuthErrorCode.CODE_EXPIRED);
        }
        if (!Objects.equals(record.getCode(), verificationCode)) {
            throw new AuthException(AuthErrorCode.CODE_MISMATCH);
        }

        record.setConsumed(true);
        record.setUpdatedAt(now);
        verificationCodeRepository.save(record);

        UserAccountEntity user = requireExistingUser(normalizedEmail, role);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Reset password for email={}, role={}", normalizedEmail, role);
    }

    public AuthSession register(String email, String password, String verificationCode, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        validatePassword(password);

        Instant now = clock.instant();
        verificationCodeRepository.deleteByExpiresAtBefore(now);
        AuthVerificationCodeEntity record = verificationCodeRepository
                .findByEmailAndRoleAndPurpose(normalizedEmail, role.alias(), VerificationPurpose.REGISTER)
                .orElseThrow(() -> new AuthException(AuthErrorCode.CODE_NOT_FOUND));
        if (record.isConsumed()) {
            throw new AuthException(AuthErrorCode.CODE_NOT_FOUND);
        }
        if (now.isAfter(record.getExpiresAt())) {
            verificationCodeRepository.deleteById(record.getCodeId());
            throw new AuthException(AuthErrorCode.CODE_EXPIRED);
        }
        if (!Objects.equals(record.getCode(), verificationCode)) {
            throw new AuthException(AuthErrorCode.CODE_MISMATCH);
        }

        record.setConsumed(true);
        record.setUpdatedAt(now);
        verificationCodeRepository.save(record);

        if (userRepository.findByEmailAndRole(normalizedEmail, role.alias()).isPresent()) {
            throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS);
        }

        String userId = UUID.randomUUID().toString();
        String hashed = passwordEncoder.encode(password);
        UserAccountEntity entity = new UserAccountEntity(userId, normalizedEmail, hashed, role.alias(), UserAccountStatus.ACTIVE);
        userRepository.save(entity);

        return createSession(entity);
    }

    public AuthSession login(String email, String password, AuthRole role) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        UserAccountEntity user = userRepository.findByEmailAndRole(normalizedEmail, role.alias())
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        user.setLastLoginAt(clock.instant());
        if (user.getStatus() == UserAccountStatus.PENDING) {
            user.setStatus(UserAccountStatus.ACTIVE);
        }
        userRepository.save(user);
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
                && userRepository.findByEmailAndRole(normalizedEmail, role.alias()).isPresent()) {
            throw new AuthException(AuthErrorCode.USER_ALREADY_EXISTS);
        }

        Instant now = clock.instant();
        verificationCodeRepository.deleteByExpiresAtBefore(now);

        AuthVerificationCodeEntity existing = verificationCodeRepository
                .findByEmailAndRoleAndPurpose(normalizedEmail, role.alias(), purpose)
                .orElse(null);
        if (existing != null && Duration.between(existing.getLastSentAt(), now).compareTo(RESEND_INTERVAL) < 0) {
            throw new AuthException(AuthErrorCode.CODE_REQUEST_TOO_FREQUENT,
                    "请在" + remainingSeconds(existing.getLastSentAt(), now) + "秒后再试");
        }

        String code = generateCode();
        Instant expireAt = now.plus(CODE_TTL);
        String requestId = UUID.randomUUID().toString();

        AuthVerificationCodeEntity entity = existing != null ? existing : new AuthVerificationCodeEntity();
        if (entity.getCodeId() == null) {
            entity.setCodeId(UUID.randomUUID().toString());
            entity.setEmail(normalizedEmail);
            entity.setRole(role.alias());
            entity.setPurpose(purpose);
            entity.setCreatedAt(now);
        }
        entity.setCode(code);
        entity.setExpiresAt(expireAt);
        entity.setConsumed(false);
        entity.setLastSentAt(now);
        entity.setRequestId(requestId);
        entity.setUpdatedAt(now);

        verificationCodeRepository.save(entity);

        log.info("Generated verification code for email={}, role={}, purpose={}, code={}",
                normalizedEmail, role, purpose, code);

        try {
            verificationCodeSender.sendVerificationCode(normalizedEmail, code, role, purpose, CODE_TTL);
        } catch (RuntimeException ex) {
            verificationCodeRepository.deleteById(entity.getCodeId());
            throw ex;
        }
        return new VerificationResult(requestId, (int) CODE_TTL.getSeconds());
    }

    private AuthSession createSession(UserAccountEntity user) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        AuthRole role = AuthRole.fromAlias(user.getRole());
        return new AuthSession(user.getUserId(), user.getEmail(), role, accessToken, refreshToken);
    }

    private long remainingSeconds(Instant lastSentAt, Instant now) {
        long elapsed = Duration.between(lastSentAt, now).getSeconds();
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

    private UserAccountEntity requireExistingUser(String email, AuthRole role) {
        return userRepository.findByEmailAndRole(email, role.alias())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND, "该邮箱尚未注册"));
    }

    public record VerificationResult(String requestId, int expiresInSeconds) {}

    public record AuthSession(String userId, String email, AuthRole role, String accessToken, String refreshToken) {}
}
