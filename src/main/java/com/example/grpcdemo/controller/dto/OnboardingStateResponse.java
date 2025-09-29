package com.example.grpcdemo.controller.dto;

import java.util.List;

/**
 * Aggregated onboarding state returned to the client for progress display.
 */
public class OnboardingStateResponse {

    private String userId;
    private int currentStep;
    private boolean completed;
    private String companyId;
    private EnterpriseCompanyInfoDto companyInfo;
    private EnterpriseContactInfoDto contactInfo;
    private EnterpriseTemplateDto templateInfo;
    private List<OnboardingStepRecordDto> records;
    private List<String> availableVariables;

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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public EnterpriseCompanyInfoDto getCompanyInfo() {
        return companyInfo;
    }

    public void setCompanyInfo(EnterpriseCompanyInfoDto companyInfo) {
        this.companyInfo = companyInfo;
    }

    public EnterpriseContactInfoDto getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(EnterpriseContactInfoDto contactInfo) {
        this.contactInfo = contactInfo;
    }

    public EnterpriseTemplateDto getTemplateInfo() {
        return templateInfo;
    }

    public void setTemplateInfo(EnterpriseTemplateDto templateInfo) {
        this.templateInfo = templateInfo;
    }

    public List<OnboardingStepRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<OnboardingStepRecordDto> records) {
        this.records = records;
    }

    public List<String> getAvailableVariables() {
        return availableVariables;
    }

    public void setAvailableVariables(List<String> availableVariables) {
        this.availableVariables = availableVariables;
    }
}
