package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.TrialInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrialInvitationRepository extends JpaRepository<TrialInvitationEntity, String> {

    Optional<TrialInvitationEntity> findTopByEmailOrderBySentAtDesc(String email);
}
