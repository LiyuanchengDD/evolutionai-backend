package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Size;

/**
 * 候选人上传头像的请求载荷。
 */
public class CandidateInterviewProfilePhotoRequest {

    @Size(max = 255, message = "文件名长度不能超过 255 字符")
    private String fileName;

    @Size(max = 100, message = "文件类型长度不能超过 100 字符")
    private String contentType;

    /**
     * Base64 编码的图片内容，PNG/JPEG 等常见格式。
     */
    private String base64Content;

    private Long sizeBytes;

    /**
     * 若为 true，表示清空原有头像。
     */
    private Boolean remove;

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

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Boolean getRemove() {
        return remove;
    }

    public void setRemove(Boolean remove) {
        this.remove = remove;
    }
}

