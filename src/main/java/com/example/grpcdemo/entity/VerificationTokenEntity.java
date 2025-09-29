package com.example.grpcdemo.entity;

import com.example.grpcdemo.onboarding.EnterpriseVerificationPurpose;
import com.example.grpcdemo.onboarding.VerificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persisted verification token for enterprise onboarding verification.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationTokenEntity {

    @Id
    @Column(name = "token_id", nullable = false, length = 36)
    private String tokenId;

    @Column(name = "target_user_id", nullable = false, length = 36)
    private String targetUserId;

    @Column(name = "target_email", nullable = false, length = 255)
    private String targetEmail;

    @Column(name = "code", nullable = false, length = 16)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private VerificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 64)
    private EnterpriseVerificationPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed", nullable = false)
    private boolean consumed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public void setTargetEmail(String targetEmail) {
        this.targetEmail = targetEmail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public VerificationChannel getChannel() {
        return channel;
    }

    public void setChannel(VerificationChannel channel) {
        this.channel = channel;
    }

    public EnterpriseVerificationPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(EnterpriseVerificationPurpose purpose) {
        this.purpose = purpose;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
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
