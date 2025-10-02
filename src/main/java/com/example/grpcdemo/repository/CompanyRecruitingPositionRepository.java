package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CompanyRecruitingPositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for recruiting positions collected during onboarding.
 */
public interface CompanyRecruitingPositionRepository extends JpaRepository<CompanyRecruitingPositionEntity, String> {

    List<CompanyRecruitingPositionEntity> findByCompanyId(String companyId);
}

