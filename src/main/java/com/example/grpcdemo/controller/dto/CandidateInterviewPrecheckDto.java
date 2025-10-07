package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

/**
 * 面试前硬件与网络检测结果。
 */
public class CandidateInterviewPrecheckDto {

    @Size(max = 32, message = "预检状态长度不能超过 32 字符")
    private String status;

    private Boolean passed;

    private Instant completedAt;

    /**
     * 原始检测报告，结构由前端/AI 检测模块约定。
     */
    private Map<String, Object> report;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Map<String, Object> getReport() {
        return report;
    }

    public void setReport(Map<String, Object> report) {
        this.report = report;
    }
}

