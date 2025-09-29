package com.example.grpcdemo.onboarding;

import org.springframework.http.HttpStatus;

/**
 * Error codes for the enterprise onboarding workflow.
 */
public enum OnboardingErrorCode {
    SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "未找到该用户的引导记录"),
    MISSING_PREVIOUS_STEP(HttpStatus.BAD_REQUEST, "请先完成上一阶段信息"),
    INVALID_TEMPLATE_VARIABLE(HttpStatus.BAD_REQUEST, "包含未支持的模版变量"),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "验证码不正确"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "验证码已过期"),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "企业资料已完成提交"),
    PERSISTENCE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "保存企业资料失败");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    OnboardingErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
