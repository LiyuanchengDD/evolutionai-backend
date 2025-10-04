package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 候选人简历原始文件与解析结果。
 */
@Entity
@Table(name = "job_candidate_resumes")
public class JobCandidateResumeEntity {

    @Id
    @Column(name = "resume_id", nullable = false, length = 36)
    private String resumeId;

    @Column(name = "job_candidate_id", nullable = false, length = 36)
    private String jobCandidateId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Lob
    @Column(name = "file_content")
    private byte[] fileContent;

    @Column(name = "parsed_name", length = 255)
    private String parsedName;

    @Column(name = "parsed_email", length = 255)
    private String parsedEmail;

    @Column(name = "parsed_phone", length = 64)
    private String parsedPhone;

    @Lob
    @Column(name = "parsed_html")
    private String parsedHtml;

    @Column(name = "confidence")
    private Float confidence;

    @Lob
    @Column(name = "ai_raw_result")
    private String aiRawResult;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getJobCandidateId() {
        return jobCandidateId;
    }

    public void setJobCandidateId(String jobCandidateId) {
        this.jobCandidateId = jobCandidateId;
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

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public String getParsedName() {
        return parsedName;
    }

    public void setParsedName(String parsedName) {
        this.parsedName = parsedName;
    }

    public String getParsedEmail() {
        return parsedEmail;
    }

    public void setParsedEmail(String parsedEmail) {
        this.parsedEmail = parsedEmail;
    }

    public String getParsedPhone() {
        return parsedPhone;
    }

    public void setParsedPhone(String parsedPhone) {
        this.parsedPhone = parsedPhone;
    }

    public String getParsedHtml() {
        return parsedHtml;
    }

    public void setParsedHtml(String parsedHtml) {
        this.parsedHtml = parsedHtml;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public String getAiRawResult() {
        return aiRawResult;
    }

    public void setAiRawResult(String aiRawResult) {
        this.aiRawResult = aiRawResult;
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
