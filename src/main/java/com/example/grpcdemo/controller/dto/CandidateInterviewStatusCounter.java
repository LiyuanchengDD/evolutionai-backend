package com.example.grpcdemo.controller.dto;

/**
 * 左侧标签的数量统计。
 */
public class CandidateInterviewStatusCounter {

    private String code;
    private String label;
    private long count;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

