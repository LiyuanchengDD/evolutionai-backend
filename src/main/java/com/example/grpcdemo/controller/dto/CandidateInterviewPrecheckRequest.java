package com.example.grpcdemo.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 候选人提交面试前检测结果的请求体。
 */
public class CandidateInterviewPrecheckRequest {

    @Valid
    private CandidateInterviewPrecheckDto summary;

    @Valid
    @NotEmpty(message = "至少需要一项检测结果")
    private List<CandidateInterviewTechnicalRequirementDto> requirements;

    public CandidateInterviewPrecheckDto getSummary() {
        return summary;
    }

    public void setSummary(CandidateInterviewPrecheckDto summary) {
        this.summary = summary;
    }

    public List<CandidateInterviewTechnicalRequirementDto> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<CandidateInterviewTechnicalRequirementDto> requirements) {
        this.requirements = requirements;
    }
}

