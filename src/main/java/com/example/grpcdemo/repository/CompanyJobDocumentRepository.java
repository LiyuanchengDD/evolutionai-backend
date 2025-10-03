package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CompanyJobDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyJobDocumentRepository extends JpaRepository<CompanyJobDocumentEntity, String> {

    Optional<CompanyJobDocumentEntity> findByPositionId(String positionId);
}
