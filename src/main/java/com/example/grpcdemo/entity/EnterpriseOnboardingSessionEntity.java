package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persisted snapshot of an in-progress enterprise onboarding session.
 */
@Entity
@Table(name = "enterprise_onboarding_sessions")
public class EnterpriseOnboardingSessionEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Lob
    @Column(name = "step1_data")
    private String step1Data;

    @Lob
    @Column(name = "step2_data")
    private String step2Data;

    @Lob
    @Column(name = "step3_data")
    private String step3Data;

    @Lob
    @Column(name = "records_data")
    private String recordsData;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public String getStep1Data() {
        return step1Data;
    }

    public void setStep1Data(String step1Data) {
        this.step1Data = step1Data;
    }

    public String getStep2Data() {
        return step2Data;
    }

    public void setStep2Data(String step2Data) {
        this.step2Data = step2Data;
    }

    public String getStep3Data() {
        return step3Data;
    }

    public void setStep3Data(String step3Data) {
        this.step3Data = step3Data;
    }

    public String getRecordsData() {
        return recordsData;
    }

    public void setRecordsData(String recordsData) {
        this.recordsData = recordsData;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
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
