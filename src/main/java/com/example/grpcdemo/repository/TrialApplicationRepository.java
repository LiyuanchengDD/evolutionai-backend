package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.ReviewStatus;
import com.example.grpcdemo.entity.TrialApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrialApplicationRepository extends JpaRepository<TrialApplicationEntity, UUID> {

    Optional<TrialApplicationEntity> findTopByApplicantUserIdAndStatusOrderByCreatedAtDesc(UUID applicantUserId, ReviewStatus status);

    List<TrialApplicationEntity> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
}
