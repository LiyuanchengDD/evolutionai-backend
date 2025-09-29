package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CompanyProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfileEntity, String> {

    Optional<CompanyProfileEntity> findByOwnerUserId(String ownerUserId);
}
