package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.AiJobExtractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiJobExtractionRepository extends JpaRepository<AiJobExtractionEntity, String> {
}
