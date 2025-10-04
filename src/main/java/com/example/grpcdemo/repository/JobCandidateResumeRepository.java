package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.JobCandidateResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 候选人简历仓储。
 */
public interface JobCandidateResumeRepository extends JpaRepository<JobCandidateResumeEntity, String> {

    Optional<JobCandidateResumeEntity> findByJobCandidateId(String jobCandidateId);
}
