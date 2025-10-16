package com.example.grpcdemo.security.trial;

import org.springframework.http.HttpStatus;

public enum TrialErrorCode {
    TRIAL_INVITE_NOT_SENT(HttpStatus.FORBIDDEN, "尚未发送邀请码"),
    TRIAL_INVITE_EXPIRED(HttpStatus.FORBIDDEN, "邀请码已经过期，请重新申请"),
    TRIAL_INVITE_INVALID_CODE(HttpStatus.BAD_REQUEST, "邀请码不正确"),
    NOT_ENTERPRISE_USER(HttpStatus.FORBIDDEN, "仅企业用户可访问"),
    ENTERPRISE_PROFILE_INCOMPLETE(HttpStatus.BAD_REQUEST, "企业资料未完善");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    TrialErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
