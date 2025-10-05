package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CandidateAiEvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * AI 评估结果仓储。
 */
public interface CandidateAiEvaluationRepository extends JpaRepository<CandidateAiEvaluationEntity, String> {

    Optional<CandidateAiEvaluationEntity> findFirstByJobCandidateIdOrderByCreatedAtDesc(String jobCandidateId);
}

