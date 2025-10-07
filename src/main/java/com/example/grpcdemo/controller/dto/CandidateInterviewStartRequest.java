package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 候选人正式进入面试房间的请求。
 */
public class CandidateInterviewStartRequest {

    @Size(max = 10, message = "语言标识长度不能超过 10 字符")
    private String locale;

    private boolean refreshQuestions;

    private Map<String, Object> context;

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isRefreshQuestions() {
        return refreshQuestions;
    }

    public void setRefreshQuestions(boolean refreshQuestions) {
        this.refreshQuestions = refreshQuestions;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}

