package com.example.grpcdemo.controller.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位候选人列表响应。
 */
public class JobCandidateListResponse {

    private List<JobCandidateItemResponse> candidates = new ArrayList<>();
    private JobCandidateStatusSummary summary;
    private long total;
    private int page;
    private int pageSize;
    private boolean hasMore;
    private Integer nextPage;

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

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public Integer getNextPage() {
        return nextPage;
    }

    public void setNextPage(Integer nextPage) {
        this.nextPage = nextPage;
    }
}
