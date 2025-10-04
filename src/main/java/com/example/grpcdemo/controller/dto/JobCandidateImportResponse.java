package com.example.grpcdemo.controller.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 候选人导入后的响应。
 */
public class JobCandidateImportResponse {

    private List<JobCandidateItemResponse> importedCandidates = new ArrayList<>();
    private JobCandidateStatusSummary summary;

    public List<JobCandidateItemResponse> getImportedCandidates() {
        return importedCandidates;
    }

    public void setImportedCandidates(List<JobCandidateItemResponse> importedCandidates) {
        this.importedCandidates = importedCandidates;
    }

    public JobCandidateStatusSummary getSummary() {
        return summary;
    }

    public void setSummary(JobCandidateStatusSummary summary) {
        this.summary = summary;
    }
}
