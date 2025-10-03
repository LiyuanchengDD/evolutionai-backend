package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Recruiting position captured during enterprise onboarding and persisted once
 * the company completes verification.
 */
@Entity
@Table(name = "company_recruiting_positions")
public class CompanyRecruitingPositionEntity {

    @Id
    @Column(name = "position_id", nullable = false, length = 36)
    private String positionId;

    @Column(name = "company_id", nullable = false, length = 36)
    private String companyId;

    @Column(name = "position_name", nullable = false, length = 255)
    private String positionName;

    @Column(name = "position_location", length = 255)
    private String positionLocation;

    @Column(name = "publisher_nickname", length = 255)
    private String publisherNickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_status", nullable = false, length = 32)
    private RecruitingPositionStatus status = RecruitingPositionStatus.READY;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_source", nullable = false, length = 32)
    private RecruitingPositionSource source = RecruitingPositionSource.MANUAL;

    @Column(name = "document_id", length = 36)
    private String documentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = RecruitingPositionStatus.READY;
        }
        if (source == null) {
            source = RecruitingPositionSource.MANUAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (status == null) {
            status = RecruitingPositionStatus.READY;
        }
        if (source == null) {
            source = RecruitingPositionSource.MANUAL;
        }
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getPositionLocation() {
        return positionLocation;
    }

    public void setPositionLocation(String positionLocation) {
        this.positionLocation = positionLocation;
    }

    public String getPublisherNickname() {
        return publisherNickname;
    }

    public void setPublisherNickname(String publisherNickname) {
        this.publisherNickname = publisherNickname;
    }

    public RecruitingPositionStatus getStatus() {
        return status;
    }

    public void setStatus(RecruitingPositionStatus status) {
        this.status = status;
    }

    public RecruitingPositionSource getSource() {
        return source;
    }

    public void setSource(RecruitingPositionSource source) {
        this.source = source;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

