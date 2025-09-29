package com.example.grpcdemo.controller.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a saved snapshot for a specific onboarding step.
 */
public class OnboardingStepRecordDto {

    private int step;
    private Instant savedAt;
    private Map<String, Object> payload;

    public OnboardingStepRecordDto() {
    }

    public OnboardingStepRecordDto(int step, Instant savedAt, Map<String, Object> payload) {
        this.step = step;
        this.savedAt = savedAt;
        this.payload = payload;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Instant savedAt) {
        this.savedAt = savedAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
