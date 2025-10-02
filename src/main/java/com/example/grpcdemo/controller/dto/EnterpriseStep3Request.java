package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for enterprise onboarding step 3 - invitation template configuration.
 */
public class EnterpriseStep3Request {

    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    @NotBlank(message = "模版名称不能为空")
    @Size(max = 255, message = "模版名称长度需小于 255 个字符")
    private String templateName;

    @NotBlank(message = "邮件主题不能为空")
    @Size(max = 255, message = "主题长度需小于 255 个字符")
    private String subject;

    @NotBlank(message = "邮件内容不能为空")
    @Size(max = 5000, message = "邮件内容长度需小于 5000 个字符")
    private String body;

    @Size(max = 32, message = "语言标识长度需小于 32 个字符")
    private String language = "zh";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
