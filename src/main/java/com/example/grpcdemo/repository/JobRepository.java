package com.example.grpcdemo.repository;

import com.example.grpcdemo.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Job} entities.
 */
public interface JobRepository extends JpaRepository<Job, String> {
}

