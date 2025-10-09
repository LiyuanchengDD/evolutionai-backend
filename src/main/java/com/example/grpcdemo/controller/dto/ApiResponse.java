package com.example.grpcdemo.controller.dto;

/**
 * 通用响应包装，保持和 AI 智能体接口文档一致的结构。
 */
public class ApiResponse<T> {

    private int code;
    private T data;
    private String message;

    public ApiResponse() {
        this.code = 200;
        this.message = "SUCCESS";
    }

    public ApiResponse(T data) {
        this();
        this.data = data;
    }

    public ApiResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
