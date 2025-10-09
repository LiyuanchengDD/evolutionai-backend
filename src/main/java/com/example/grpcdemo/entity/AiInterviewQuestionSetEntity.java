package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 面试问题生成历史记录。
 */
@Entity
@Table(name = "ai_interview_question_sets")
public class AiInterviewQuestionSetEntity {

    @Id
    @Column(name = "record_id", nullable = false, length = 36)
    private String recordId;

    @Column(name = "resume_url", nullable = false, length = 1000)
    private String resumeUrl;

    @Column(name = "jd_url", nullable = false, length = 1000)
    private String jdUrl;

    @Column(name = "question_num")
    private Integer questionNum;

    @Lob
    @Column(name = "questions_json")
    private String questionsJson;

    @Column(name = "candidate_name", length = 128)
    private String candidateName;

    @Column(name = "candidate_email", length = 255)
    private String candidateEmail;

    @Column(name = "job_title", length = 255)
    private String jobTitle;

    @Column(name = "job_location", length = 255)
    private String jobLocation;

    @Lob
    @Column(name = "resume_snapshot")
    private String resumeSnapshot;

    @Lob
    @Column(name = "jd_snapshot")
    private String jdSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getJdUrl() {
        return jdUrl;
    }

    public void setJdUrl(String jdUrl) {
        this.jdUrl = jdUrl;
    }

    public Integer getQuestionNum() {
        return questionNum;
    }

    public void setQuestionNum(Integer questionNum) {
        this.questionNum = questionNum;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
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

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(String jobLocation) {
        this.jobLocation = jobLocation;
    }

    public String getResumeSnapshot() {
        return resumeSnapshot;
    }

    public void setResumeSnapshot(String resumeSnapshot) {
        this.resumeSnapshot = resumeSnapshot;
    }

    public String getJdSnapshot() {
        return jdSnapshot;
    }

    public void setJdSnapshot(String jdSnapshot) {
        this.jdSnapshot = jdSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
