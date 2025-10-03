package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link UserAccountEntity} records.
 */
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {

    Optional<UserAccountEntity> findByEmailAndRole(String email, String role);

    Optional<UserAccountEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, String userId);
}
