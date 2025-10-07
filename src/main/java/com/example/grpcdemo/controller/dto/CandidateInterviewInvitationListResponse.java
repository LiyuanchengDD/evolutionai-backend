package com.example.grpcdemo.controller.dto;

import java.util.List;

/**
 * 面试邀约列表响应。
 */
public class CandidateInterviewInvitationListResponse {

    private List<CandidateInterviewInvitationItem> invitations;
    private List<CandidateInterviewStatusCounter> statusCounters;
    private long total;
    private String activeStatus;
    private String keyword;

    public List<CandidateInterviewInvitationItem> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<CandidateInterviewInvitationItem> invitations) {
        this.invitations = invitations;
    }

    public List<CandidateInterviewStatusCounter> getStatusCounters() {
        return statusCounters;
    }

    public void setStatusCounters(List<CandidateInterviewStatusCounter> statusCounters) {
        this.statusCounters = statusCounters;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}

