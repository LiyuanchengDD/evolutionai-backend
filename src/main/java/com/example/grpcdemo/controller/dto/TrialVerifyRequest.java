package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class TrialVerifyRequest {

    @NotBlank(message = "邀请码不能为空")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
