package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 企业岗位下的候选人记录。
 */
@Entity
@Table(name = "job_candidates")
public class CompanyJobCandidateEntity {

    @Id
    @Column(name = "job_candidate_id", nullable = false, length = 36)
    private String jobCandidateId;

    @Column(name = "position_id", nullable = false, length = 36)
    private String positionId;

    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    @Column(name = "candidate_email", length = 255)
    private String candidateEmail;

    @Column(name = "candidate_phone", length = 64)
    private String candidatePhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false, length = 32)
    private JobCandidateInviteStatus inviteStatus = JobCandidateInviteStatus.INVITE_PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_status", nullable = false, length = 32)
    private JobCandidateInterviewStatus interviewStatus = JobCandidateInterviewStatus.NOT_INTERVIEWED;

    @Column(name = "resume_id", length = 36)
    private String resumeId;

    @Column(name = "uploader_user_id", length = 36)
    private String uploaderUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (inviteStatus == null) {
            inviteStatus = JobCandidateInviteStatus.INVITE_PENDING;
        }
        if (interviewStatus == null) {
            interviewStatus = JobCandidateInterviewStatus.NOT_INTERVIEWED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (inviteStatus == null) {
            inviteStatus = JobCandidateInviteStatus.INVITE_PENDING;
        }
        if (interviewStatus == null) {
            interviewStatus = JobCandidateInterviewStatus.NOT_INTERVIEWED;
        }
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

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public String getCandidatePhone() {
        return candidatePhone;
    }

    public void setCandidatePhone(String candidatePhone) {
        this.candidatePhone = candidatePhone;
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

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getUploaderUserId() {
        return uploaderUserId;
    }

    public void setUploaderUserId(String uploaderUserId) {
        this.uploaderUserId = uploaderUserId;
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
