package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;

import java.time.Instant;

/**
 * 候选人列表展示项。
 */
public class JobCandidateItemResponse {

    private String jobCandidateId;
    private String positionId;
    private String name;
    private String email;
    private String phone;
    private JobCandidateInviteStatus inviteStatus;
    private JobCandidateInterviewStatus interviewStatus;
    private boolean emailMissing;
    private boolean resumeAvailable;
    private boolean interviewRecordAvailable;
    private boolean aiEvaluationAvailable;
    private Instant interviewCompletedAt;
    private Instant interviewDeadlineAt;
    private Instant createdAt;
    private Instant updatedAt;

    public JobCandidateItemResponse() {
    }

    public JobCandidateItemResponse(String jobCandidateId,
                                    String positionId,
                                    String name,
                                    String email,
                                    String phone,
                                    JobCandidateInviteStatus inviteStatus,
                                    JobCandidateInterviewStatus interviewStatus,
                                    boolean emailMissing,
                                    boolean resumeAvailable,
                                    boolean interviewRecordAvailable,
                                    boolean aiEvaluationAvailable,
                                    Instant interviewCompletedAt,
                                    Instant interviewDeadlineAt,
                                    Instant createdAt,
                                    Instant updatedAt) {
        this.jobCandidateId = jobCandidateId;
        this.positionId = positionId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.inviteStatus = inviteStatus;
        this.interviewStatus = interviewStatus;
        this.emailMissing = emailMissing;
        this.resumeAvailable = resumeAvailable;
        this.interviewRecordAvailable = interviewRecordAvailable;
        this.aiEvaluationAvailable = aiEvaluationAvailable;
        this.interviewCompletedAt = interviewCompletedAt;
        this.interviewDeadlineAt = interviewDeadlineAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getJobCandidateId() {
        return jobCandidateId;
    }

    public void setJobCandidateId(String jobCandidateId) {
        this.jobCandidateId = jobCandidateId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public JobCandidateInviteStatus getInviteStatus() {
        return inviteStatus;
    }

    public void setInviteStatus(JobCandidateInviteStatus inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public JobCandidateInterviewStatus getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(JobCandidateInterviewStatus interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public boolean isEmailMissing() {
        return emailMissing;
    }

    public void setEmailMissing(boolean emailMissing) {
        this.emailMissing = emailMissing;
    }

    public boolean isResumeAvailable() {
        return resumeAvailable;
    }

    public void setResumeAvailable(boolean resumeAvailable) {
        this.resumeAvailable = resumeAvailable;
    }

    public boolean isInterviewRecordAvailable() {
        return interviewRecordAvailable;
    }

    public void setInterviewRecordAvailable(boolean interviewRecordAvailable) {
        this.interviewRecordAvailable = interviewRecordAvailable;
    }

    public boolean isAiEvaluationAvailable() {
        return aiEvaluationAvailable;
    }

    public void setAiEvaluationAvailable(boolean aiEvaluationAvailable) {
        this.aiEvaluationAvailable = aiEvaluationAvailable;
    }

    public Instant getInterviewCompletedAt() {
        return interviewCompletedAt;
    }

    public void setInterviewCompletedAt(Instant interviewCompletedAt) {
        this.interviewCompletedAt = interviewCompletedAt;
    }

    public Instant getInterviewDeadlineAt() {
        return interviewDeadlineAt;
    }

    public void setInterviewDeadlineAt(Instant interviewDeadlineAt) {
        this.interviewDeadlineAt = interviewDeadlineAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
