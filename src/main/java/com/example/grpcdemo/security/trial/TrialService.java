package com.example.grpcdemo.security.trial;

import com.example.grpcdemo.entity.TrialInvitationEntity;
import com.example.grpcdemo.repository.TrialInvitationRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Component
public class TrialService {

    private static final Duration TRIAL_TTL = Duration.ofDays(14);

    private final TrialInvitationRepository trialInvitationRepository;
    private final Clock clock;

    public TrialService(TrialInvitationRepository trialInvitationRepository, Clock clock) {
        this.trialInvitationRepository = trialInvitationRepository;
        this.clock = clock;
    }

    public TrialStatus evaluate(String email) {
        if (email == null || email.isBlank()) {
            return new TrialStatus(TrialStatusCode.NOT_SENT, null);
        }
        String normalized = email.toLowerCase(Locale.ROOT);
        return trialInvitationRepository.findTopByEmailOrderBySentAtDesc(normalized)
                .map(this::toStatus)
                .orElseGet(() -> new TrialStatus(TrialStatusCode.NOT_SENT, null));
    }

    private TrialStatus toStatus(TrialInvitationEntity entity) {
        Instant sentAt = entity.getSentAt();
        Instant expiresAt = sentAt.plus(TRIAL_TTL);
        Instant now = clock.instant();
        if (now.isAfter(expiresAt)) {
            return new TrialStatus(TrialStatusCode.EXPIRED, expiresAt);
        }
        return new TrialStatus(TrialStatusCode.ACTIVE, expiresAt);
    }
}

