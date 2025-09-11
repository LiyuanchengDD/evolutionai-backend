package com.example.grpcdemo.repository;

import com.example.grpcdemo.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Candidate} entities.
 */
public interface CandidateRepository extends JpaRepository<Candidate, String> {
}

