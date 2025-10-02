package com.example.grpcdemo.repository;

import com.example.grpcdemo.auth.VerificationPurpose;
import com.example.grpcdemo.entity.AuthVerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AuthVerificationCodeRepository extends JpaRepository<AuthVerificationCodeEntity, String> {

    Optional<AuthVerificationCodeEntity> findByEmailAndRoleAndPurpose(String email,
                                                                     String role,
                                                                     VerificationPurpose purpose);

    Optional<AuthVerificationCodeEntity> findByRequestId(String requestId);

    void deleteByExpiresAtBefore(Instant cutoff);
}
