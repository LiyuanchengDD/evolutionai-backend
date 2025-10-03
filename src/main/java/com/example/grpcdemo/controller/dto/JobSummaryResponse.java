package com.example.grpcdemo.controller.dto;

/**
 * 岗位模块概览信息，用于判断是否展示首次引导。
 */
public class JobSummaryResponse {

    private final String companyId;
    private final long totalPositions;
    private final boolean shouldShowOnboarding;

    public JobSummaryResponse(String companyId, long totalPositions, boolean shouldShowOnboarding) {
        this.companyId = companyId;
        this.totalPositions = totalPositions;
        this.shouldShowOnboarding = shouldShowOnboarding;
    }

    public String getCompanyId() {
        return companyId;
    }

    public long getTotalPositions() {
        return totalPositions;
    }

    public boolean isShouldShowOnboarding() {
        return shouldShowOnboarding;
    }
}
