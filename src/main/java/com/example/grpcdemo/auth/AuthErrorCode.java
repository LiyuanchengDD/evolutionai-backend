package com.example.grpcdemo.auth;

import io.grpc.Status;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode {
    INVALID_ROLE(Status.INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, "无效的角色"),
    INVALID_EMAIL(Status.INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, "邮箱格式不正确"),
    CODE_REQUEST_TOO_FREQUENT(Status.RESOURCE_EXHAUSTED, HttpStatus.TOO_MANY_REQUESTS, "验证码请求过于频繁"),
    CODE_NOT_FOUND(Status.NOT_FOUND, HttpStatus.BAD_REQUEST, "验证码不存在或已被使用"),
    CODE_EXPIRED(Status.DEADLINE_EXCEEDED, HttpStatus.BAD_REQUEST, "验证码已过期"),
    CODE_MISMATCH(Status.PERMISSION_DENIED, HttpStatus.BAD_REQUEST, "验证码不匹配"),
    USER_ALREADY_EXISTS(Status.ALREADY_EXISTS, HttpStatus.CONFLICT, "用户已存在"),
    USER_NOT_FOUND(Status.NOT_FOUND, HttpStatus.NOT_FOUND, "用户不存在"),
    PASSWORD_TOO_WEAK(Status.INVALID_ARGUMENT, HttpStatus.BAD_REQUEST, "密码强度不足"),
    INVALID_CREDENTIALS(Status.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED, "邮箱或密码错误"),
    INTERNAL_ERROR(Status.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");

    private final Status status;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    AuthErrorCode(Status status, HttpStatus httpStatus, String defaultMessage) {
        this.status = status;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public Status getStatus() {
        return status;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
