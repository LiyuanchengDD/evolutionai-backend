package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthResetPasswordRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,
        @NotBlank(message = "验证码不能为空")
        String verificationCode,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "密码至少需要6位")
        String newPassword,
        @NotBlank(message = "角色不能为空")
        String role
) {
}
