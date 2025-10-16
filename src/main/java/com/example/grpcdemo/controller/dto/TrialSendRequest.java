package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Email;

public class TrialSendRequest {

    private String applicationId;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String companyId;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }
}
