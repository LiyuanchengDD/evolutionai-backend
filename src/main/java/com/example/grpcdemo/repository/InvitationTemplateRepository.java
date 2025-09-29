package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.InvitationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationTemplateRepository extends JpaRepository<InvitationTemplateEntity, String> {

    Optional<InvitationTemplateEntity> findFirstByCompanyIdAndDefaultTemplateTrue(String companyId);
}
