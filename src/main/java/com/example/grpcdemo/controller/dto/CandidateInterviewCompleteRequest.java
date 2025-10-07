package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * 候选人完成所有题目后的提交。
 */
public class CandidateInterviewCompleteRequest {

    @Min(value = 1, message = "总时长必须大于 0")
    @Max(value = 14400, message = "总时长不能超过 4 小时")
    private Integer durationSeconds;

    private Map<String, Object> metadata;

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

