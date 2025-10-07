package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 面试前上传的人像照片元数据。
 */
public class CandidateInterviewProfilePhotoDto {

    @Size(max = 255, message = "文件名长度不能超过 255 字符")
    private String fileName;

    @Size(max = 100, message = "文件类型长度不能超过 100 字符")
    private String contentType;

    private Long sizeBytes;

    private Instant uploadedAt;

    private String downloadUrl;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}

