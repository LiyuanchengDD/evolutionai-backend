package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.ReviewStatus;
import com.example.grpcdemo.entity.TrialApplicationEntity;
import com.example.grpcdemo.repository.TrialApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TrialApplicationService {

    private final TrialApplicationRepository trialApplicationRepository;
    private final Clock clock;

    public TrialApplicationService(TrialApplicationRepository trialApplicationRepository, Clock clock) {
        this.trialApplicationRepository = trialApplicationRepository;
        this.clock = clock;
    }

    @Transactional
    public TrialApplicationEntity create(UUID applicantUserId,
                                         String companyName,
                                         String contactEmail,
                                         String reason) {
        Optional<TrialApplicationEntity> existing = trialApplicationRepository
                .findTopByApplicantUserIdAndStatusOrderByCreatedAtDesc(applicantUserId, ReviewStatus.PENDING);
        if (existing.isPresent()) {
            return existing.get();
        }
        TrialApplicationEntity entity = new TrialApplicationEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplicantUserId(applicantUserId);
        entity.setCompanyName(companyName);
        entity.setContactEmail(contactEmail);
        entity.setReason(reason);
        entity.setStatus(ReviewStatus.PENDING);
        return trialApplicationRepository.save(entity);
    }

    public List<TrialApplicationEntity> findByStatus(ReviewStatus status) {
        return trialApplicationRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<TrialApplicationEntity> findById(UUID id) {
        return trialApplicationRepository.findById(id);
    }

    @Transactional
    public TrialApplicationEntity review(UUID id, boolean approve, UUID reviewerId, String note) {
        TrialApplicationEntity entity = trialApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("试用申请不存在"));
        if (entity.getStatus() != ReviewStatus.PENDING) {
            return entity;
        }
        entity.setStatus(approve ? ReviewStatus.APPROVED : ReviewStatus.REJECTED);
        entity.setReviewedBy(reviewerId);
        entity.setReviewedAt(Instant.now(clock));
        entity.setNote(note);
        return trialApplicationRepository.save(entity);
    }
}
