package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Size;

/**
 * 面试进入前需要通过的技术检查项。
 */
public class CandidateInterviewTechnicalRequirementDto {

    @Size(max = 64, message = "检查项编码长度不能超过 64 字符")
    private String code;

    @Size(max = 255, message = "检查项名称长度不能超过 255 字符")
    private String name;

    private String description;

    private Boolean passed;

    private String message;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

