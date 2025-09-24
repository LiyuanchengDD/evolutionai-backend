package com.example.grpcdemo.repository;

import com.example.grpcdemo.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link Interview} entities.
 */
public interface InterviewRepository extends JpaRepository<Interview, String> {

    List<Interview> findByCandidateId(String candidateId);

    List<Interview> findByJobId(String jobId);
}

