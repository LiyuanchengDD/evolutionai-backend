package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CandidateInterviewRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 面试记录仓储。
 */
public interface CandidateInterviewRecordRepository extends JpaRepository<CandidateInterviewRecordEntity, String> {

    Optional<CandidateInterviewRecordEntity> findFirstByJobCandidateIdOrderByCreatedAtDesc(String jobCandidateId);
}

