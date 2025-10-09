package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.AiQuestionTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiQuestionTemplateRepository extends JpaRepository<AiQuestionTemplateEntity, UUID> {

    List<AiQuestionTemplateEntity> findByLanguageAndActiveTrueOrderByDisplayOrderAsc(String language);

    List<AiQuestionTemplateEntity> findByActiveTrueOrderByDisplayOrderAsc();
}

