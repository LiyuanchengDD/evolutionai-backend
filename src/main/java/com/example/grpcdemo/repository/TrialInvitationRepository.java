package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.TrialInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TrialInvitationRepository extends JpaRepository<TrialInvitationEntity, UUID> {

    Optional<TrialInvitationEntity> findTopByEmailOrderBySentAtDesc(String email);

    Optional<TrialInvitationEntity> findTopByCompanyIdOrderBySentAtDesc(UUID companyId);

    Optional<TrialInvitationEntity> findTopByCompanyIdAndSentAtAfterOrderBySentAtDesc(UUID companyId, Instant since);
}
