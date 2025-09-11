package com.example.grpcdemo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * JPA entity representing a stored report.
 */
@Entity
@Table(name = "reports")
public class ReportEntity {

    @Id
    private String reportId;
    private String interviewId;

    @Lob
    private String content;

    private float score;
    private String evaluatorComment;
    private long createdAt;

    public ReportEntity() {
    }

    public ReportEntity(String reportId, String interviewId, String content,
                        float score, String evaluatorComment, long createdAt) {
        this.reportId = reportId;
        this.interviewId = interviewId;
        this.content = content;
        this.score = score;
        this.evaluatorComment = evaluatorComment;
        this.createdAt = createdAt;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getEvaluatorComment() {
        return evaluatorComment;
    }

    public void setEvaluatorComment(String evaluatorComment) {
        this.evaluatorComment = evaluatorComment;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

