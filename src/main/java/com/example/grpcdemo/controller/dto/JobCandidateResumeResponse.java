package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;

import java.time.Instant;

/**
 * 候选人简历详情响应。
 */
public class JobCandidateResumeResponse {

    private String jobCandidateId;
    private String positionId;
    private String name;
    private String email;
    private String phone;
    private JobCandidateInviteStatus inviteStatus;
    private JobCandidateInterviewStatus interviewStatus;
    private String resumeHtml;
    private String resumeDocumentHtml;
    private String fileName;
    private String fileType;
    private Instant uploadedAt;
    private Float confidence;

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

    public String getResumeHtml() {
        return resumeHtml;
    }

    public void setResumeHtml(String resumeHtml) {
        this.resumeHtml = resumeHtml;
    }

    public String getResumeDocumentHtml() {
        return resumeDocumentHtml;
    }

    public void setResumeDocumentHtml(String resumeDocumentHtml) {
        this.resumeDocumentHtml = resumeDocumentHtml;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }
}
