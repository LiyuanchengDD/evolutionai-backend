package com.example.grpcdemo.security.trial;

import com.example.grpcdemo.config.AppTrialProperties;
import com.example.grpcdemo.entity.TrialInvitationEntity;
import com.example.grpcdemo.entity.UserProfileEntity;
import com.example.grpcdemo.repository.TrialInvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class TrialService {

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final TrialInvitationRepository trialInvitationRepository;
    private final Clock clock;
    private final AppTrialProperties properties;
    private final SecureRandom secureRandom;

    public TrialService(TrialInvitationRepository trialInvitationRepository,
                        Clock clock,
                        AppTrialProperties properties,
                        SecureRandom secureRandom) {
        this.trialInvitationRepository = trialInvitationRepository;
        this.clock = clock;
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public TrialStatus evaluate(UserProfileEntity profile) {
        if (profile == null || profile.getCompanyId() == null) {
            return new TrialStatus(TrialStatusCode.NOT_SENT, null, null, null);
        }
        return trialInvitationRepository.findTopByCompanyIdOrderBySentAtDesc(profile.getCompanyId())
                .map(this::toStatus)
                .orElseGet(() -> new TrialStatus(TrialStatusCode.NOT_SENT, null, null, null));
    }

    public TrialStatus evaluateByEmail(String email) {
        if (email == null || email.isBlank()) {
            return new TrialStatus(TrialStatusCode.NOT_SENT, null, null, null);
        }
        String normalized = email.toLowerCase(Locale.ROOT);
        return trialInvitationRepository.findTopByEmailOrderBySentAtDesc(normalized)
                .map(this::toStatus)
                .orElseGet(() -> new TrialStatus(TrialStatusCode.NOT_SENT, null, null, null));
    }

    @Transactional
    public TrialStatus markRedeemed(UserProfileEntity profile) {
        if (profile == null || profile.getCompanyId() == null) {
            return new TrialStatus(TrialStatusCode.NOT_SENT, null, null, null);
        }
        Optional<TrialInvitationEntity> invitationOpt = trialInvitationRepository
                .findTopByCompanyIdOrderBySentAtDesc(profile.getCompanyId());
        if (invitationOpt.isEmpty()) {
            return new TrialStatus(TrialStatusCode.NOT_SENT, null, null, null);
        }
        TrialInvitationEntity entity = invitationOpt.get();
        if (entity.getRedeemedAt() == null) {
            entity.setRedeemedAt(clock.instant());
            entity.setRedeemedBy(profile.getId());
            trialInvitationRepository.save(entity);
            return toStatus(entity);
        }
        return toStatus(entity);
    }

    @Transactional
    public TrialStatus verifyCode(UserProfileEntity profile, String code, String email) {
        if (profile == null || profile.getCompanyId() == null) {
            throw TrialExceptions.invalidEnterpriseProfile();
        }
        if (code == null || code.isBlank()) {
            throw TrialExceptions.invalidCode();
        }
        String normalizedEmail = email != null ? email.toLowerCase(Locale.ROOT) : null;
        Instant now = clock.instant();

        if (properties.isDevFixedMode()) {
            if (!constantTimeEquals(properties.getDev().getCode(), code)) {
                throw TrialExceptions.invalidCode();
            }
            TrialInvitationEntity entity = trialInvitationRepository
                    .findTopByCompanyIdOrderBySentAtDesc(profile.getCompanyId())
                    .orElseGet(() -> createInvitation(profile.getCompanyId(), normalizedEmail));
            entity.setEmail(normalizedEmail);
            entity.setCode(properties.getDev().getCode());
            entity.setSentAt(now);
            entity.setRedeemedAt(now);
            entity.setRedeemedBy(profile.getId());
            trialInvitationRepository.save(entity);
            return toStatus(entity);
        }

        TrialInvitationEntity entity = trialInvitationRepository
                .findTopByCompanyIdOrderBySentAtDesc(profile.getCompanyId())
                .orElseThrow(TrialExceptions::inviteNotSent);
        TrialStatus status = toStatus(entity);
        if (status.status() == TrialStatusCode.EXPIRED) {
            throw TrialExceptions.inviteExpired();
        }
        if (!constantTimeEquals(entity.getCode(), code)) {
            throw TrialExceptions.invalidCode();
        }
        if (entity.getRedeemedAt() == null) {
            entity.setRedeemedAt(now);
            entity.setRedeemedBy(profile.getId());
            trialInvitationRepository.save(entity);
            return toStatus(entity);
        }
        return status;
    }

    @Transactional
    public TrialSendResult sendInvitation(UUID companyId, String email) {
        if (companyId == null) {
            throw TrialExceptions.invalidEnterpriseProfile();
        }
        String normalizedEmail = email != null ? email.toLowerCase(Locale.ROOT) : null;
        Instant now = clock.instant();
        Instant threshold = now.minus(getTrialDuration());
        Optional<TrialInvitationEntity> existing = trialInvitationRepository
                .findTopByCompanyIdAndSentAtAfterOrderBySentAtDesc(companyId, threshold);
        if (existing.isPresent()) {
            TrialInvitationEntity entity = existing.get();
            TrialStatus status = toStatus(entity);
            return new TrialSendResult(status, properties.isDevFixedMode() ? properties.getDev().getCode() : null, false);
        }

        TrialInvitationEntity entity = createInvitation(companyId, normalizedEmail);
        entity.setEmail(normalizedEmail);
        entity.setSentAt(now);
        if (properties.isDevFixedMode()) {
            entity.setCode(properties.getDev().getCode());
        } else {
            entity.setCode(generateCode());
        }
        trialInvitationRepository.save(entity);
        TrialStatus status = toStatus(entity);
        return new TrialSendResult(status, properties.isDevFixedMode() ? entity.getCode() : null, true);
    }

    private TrialInvitationEntity createInvitation(UUID companyId, String email) {
        TrialInvitationEntity entity = new TrialInvitationEntity();
        entity.setId(UUID.randomUUID());
        entity.setCompanyId(companyId);
        entity.setEmail(email);
        entity.setSentAt(clock.instant());
        return entity;
    }

    private Duration getTrialDuration() {
        return Duration.ofDays(Math.max(properties.getValidDays(), 1));
    }

    private TrialStatus toStatus(TrialInvitationEntity entity) {
        Instant sentAt = entity.getSentAt();
        Instant expiresAt = sentAt != null ? sentAt.plus(getTrialDuration()) : null;
        Instant redeemedAt = entity.getRedeemedAt();
        if (redeemedAt != null) {
            return new TrialStatus(TrialStatusCode.REDEEMED, sentAt, expiresAt, redeemedAt);
        }
        Instant now = clock.instant();
        if (expiresAt != null && now.isAfter(expiresAt)) {
            return new TrialStatus(TrialStatusCode.EXPIRED, sentAt, expiresAt, redeemedAt);
        }
        return new TrialStatus(TrialStatusCode.VALID, sentAt, expiresAt, redeemedAt);
    }

    private boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        byte[] a = expected.getBytes();
        byte[] b = provided.getBytes();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] digestA = digest.digest(a);
            byte[] digestB = digest.digest(b);
            return MessageDigest.isEqual(digestA, digestB);
        } catch (NoSuchAlgorithmException e) {
            return expected.equals(provided);
        }
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            int index = secureRandom.nextInt(CODE_ALPHABET.length);
            builder.append(CODE_ALPHABET[index]);
        }
        return builder.toString();
    }

    public record TrialSendResult(TrialStatus status, String code, boolean newlyCreated) {
    }
}
