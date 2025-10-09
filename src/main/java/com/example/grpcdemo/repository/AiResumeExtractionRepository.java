package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.AiResumeExtractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiResumeExtractionRepository extends JpaRepository<AiResumeExtractionEntity, String> {
}
