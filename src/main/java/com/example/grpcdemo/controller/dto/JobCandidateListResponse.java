package com.example.grpcdemo.controller.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位候选人列表响应。
 */
public class JobCandidateListResponse {

    private List<JobCandidateItemResponse> candidates = new ArrayList<>();
    private JobCandidateStatusSummary summary;

    public List<JobCandidateItemResponse> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<JobCandidateItemResponse> candidates) {
        this.candidates = candidates;
    }

    public JobCandidateStatusSummary getSummary() {
        return summary;
    }

    public void setSummary(JobCandidateStatusSummary summary) {
        this.summary = summary;
    }
}
