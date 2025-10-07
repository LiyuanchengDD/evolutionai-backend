package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 候选人主动放弃面试的请求。
 */
public class CandidateInterviewAbandonRequest {

    @Size(max = 500, message = "放弃原因不能超过 500 字符")
    private String reason;

    private Map<String, Object> metadata;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

