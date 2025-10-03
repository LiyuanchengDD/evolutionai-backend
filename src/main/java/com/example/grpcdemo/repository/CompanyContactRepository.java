package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CompanyContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyContactRepository extends JpaRepository<CompanyContactEntity, String> {

    Optional<CompanyContactEntity> findFirstByCompanyIdOrderByCreatedAtAsc(String companyId);

    List<CompanyContactEntity> findByCompanyIdOrderByCreatedAtAsc(String companyId);

    Optional<CompanyContactEntity> findByContactIdAndCompanyId(String contactId, String companyId);

    List<CompanyContactEntity> findByCompanyId(String companyId);

    Optional<CompanyContactEntity> findByUserAccountId(String userAccountId);
}
