package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.VerificationTokenEntity;
import com.example.grpcdemo.onboarding.EnterpriseVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, String> {

    Optional<VerificationTokenEntity> findTopByTargetUserIdAndPurposeAndCodeAndConsumedFalseOrderByCreatedAtDesc(
            String targetUserId,
            EnterpriseVerificationPurpose purpose,
            String code
    );

    @Query("select token from VerificationTokenEntity token " +
            "where token.targetUserId = :targetUserId " +
            "and token.purpose = :purpose " +
            "and token.consumed = false " +
            "and token.expiresAt >= :now " +
            "order by token.createdAt desc")
    Optional<VerificationTokenEntity> findLatestValidToken(@Param("targetUserId") String targetUserId,
                                                           @Param("purpose") EnterpriseVerificationPurpose purpose,
                                                           @Param("now") Instant now);
}
