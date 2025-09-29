package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for verifying the onboarding verification code and persisting data.
 */
public class EnterpriseVerifyRequest {

    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^[0-9A-Za-z]{4,8}$", message = "验证码格式不正确")
    private String verificationCode;

    @Size(max = 255, message = "验证邮箱长度需小于 255 个字符")
    private String email;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
