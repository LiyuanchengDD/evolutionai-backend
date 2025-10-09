package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 文件信息提取请求。
 */
public class AiExtractionRequest {

    @NotBlank(message = "文件地址不能为空")
    private String fileUrl;

    @NotBlank(message = "提取类型不能为空")
    private String extractType;

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getExtractType() {
        return extractType;
    }

    public void setExtractType(String extractType) {
        this.extractType = extractType;
    }
}
