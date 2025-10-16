package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trial_invitations")
public class TrialInvitationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "code", nullable = false, length = 255)
    private String code;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "redeemed_by")
    private UUID redeemedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(Instant redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public UUID getRedeemedBy() {
        return redeemedBy;
    }

    public void setRedeemedBy(UUID redeemedBy) {
        this.redeemedBy = redeemedBy;
    }
}
