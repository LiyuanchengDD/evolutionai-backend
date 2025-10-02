package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.EnterpriseOnboardingSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface EnterpriseOnboardingSessionRepository extends JpaRepository<EnterpriseOnboardingSessionEntity, String> {

    void deleteByExpiresAtBefore(Instant cutoff);
}
