package com.example.grpcdemo.service;

/**
 * 自定义异常用于标识岗位解析失败。
 */
public class JobParsingException extends RuntimeException {

    public JobParsingException(String message) {
        super(message);
    }

    public JobParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
