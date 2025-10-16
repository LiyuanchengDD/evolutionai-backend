package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TrialApplyRequest {

    @NotBlank(message = "企业名称不能为空")
    private String companyName;

    @NotBlank(message = "联系人邮箱不能为空")
    @Email(message = "联系人邮箱格式不正确")
    private String contactEmail;

    @Size(max = 2000, message = "申请理由长度超出限制")
    private String reason;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
