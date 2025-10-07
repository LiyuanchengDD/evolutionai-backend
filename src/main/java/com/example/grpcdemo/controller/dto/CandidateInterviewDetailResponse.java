package com.example.grpcdemo.controller.dto;

import java.time.Instant;
import java.util.List;

/**
 * 候选人点击邀约卡片后的右侧详情。
 */
public class CandidateInterviewDetailResponse {

    private CandidateInterviewInvitationItem invitation;
    private CandidateInterviewRecordResponse record;
    private List<CandidateInterviewTechnicalRequirementDto> requirements;
    private Instant answerDeadlineAt;
    private boolean readyForInterview;
    private boolean precheckPassed;
    private boolean profilePhotoUploaded;

    public CandidateInterviewInvitationItem getInvitation() {
        return invitation;
    }

    public void setInvitation(CandidateInterviewInvitationItem invitation) {
        this.invitation = invitation;
    }

    public CandidateInterviewRecordResponse getRecord() {
        return record;
    }

    public void setRecord(CandidateInterviewRecordResponse record) {
        this.record = record;
    }

    public List<CandidateInterviewTechnicalRequirementDto> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<CandidateInterviewTechnicalRequirementDto> requirements) {
        this.requirements = requirements;
    }

    public Instant getAnswerDeadlineAt() {
        return answerDeadlineAt;
    }

    public void setAnswerDeadlineAt(Instant answerDeadlineAt) {
        this.answerDeadlineAt = answerDeadlineAt;
    }

    public boolean isReadyForInterview() {
        return readyForInterview;
    }

    public void setReadyForInterview(boolean readyForInterview) {
        this.readyForInterview = readyForInterview;
    }

    public boolean isPrecheckPassed() {
        return precheckPassed;
    }

    public void setPrecheckPassed(boolean precheckPassed) {
        this.precheckPassed = precheckPassed;
    }

    public boolean isProfilePhotoUploaded() {
        return profilePhotoUploaded;
    }

    public void setProfilePhotoUploaded(boolean profilePhotoUploaded) {
        this.profilePhotoUploaded = profilePhotoUploaded;
    }
}

