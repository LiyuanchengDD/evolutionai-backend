package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link ReportEntity}.
 */
public interface ReportRepository extends JpaRepository<ReportEntity, String> {
}

