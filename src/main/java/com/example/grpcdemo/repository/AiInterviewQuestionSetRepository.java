package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.AiInterviewQuestionSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInterviewQuestionSetRepository extends JpaRepository<AiInterviewQuestionSetEntity, String> {
}
