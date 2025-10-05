package com.example.grpcdemo.controller.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 面试记录查询响应。
 */
public class CandidateInterviewRecordResponse {

    private String interviewRecordId;
    private String jobCandidateId;
    private String interviewMode;
    private String interviewerName;
    private String aiSessionId;
    private Instant interviewStartedAt;
    private Instant interviewEndedAt;
    private Integer durationSeconds;
    private List<CandidateInterviewQuestionDto> questions;
    private String transcriptRaw;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public String getInterviewRecordId() {
        return interviewRecordId;
    }

    public void setInterviewRecordId(String interviewRecordId) {
        this.interviewRecordId = interviewRecordId;
    }

    public String getJobCandidateId() {
        return jobCandidateId;
    }

    public void setJobCandidateId(String jobCandidateId) {
        this.jobCandidateId = jobCandidateId;
    }

    public String getInterviewMode() {
        return interviewMode;
    }

    public void setInterviewMode(String interviewMode) {
        this.interviewMode = interviewMode;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public String getAiSessionId() {
        return aiSessionId;
    }

    public void setAiSessionId(String aiSessionId) {
        this.aiSessionId = aiSessionId;
    }

    public Instant getInterviewStartedAt() {
        return interviewStartedAt;
    }

    public void setInterviewStartedAt(Instant interviewStartedAt) {
        this.interviewStartedAt = interviewStartedAt;
    }

    public Instant getInterviewEndedAt() {
        return interviewEndedAt;
    }

    public void setInterviewEndedAt(Instant interviewEndedAt) {
        this.interviewEndedAt = interviewEndedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public List<CandidateInterviewQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<CandidateInterviewQuestionDto> questions) {
        this.questions = questions;
    }

    public String getTranscriptRaw() {
        return transcriptRaw;
    }

    public void setTranscriptRaw(String transcriptRaw) {
        this.transcriptRaw = transcriptRaw;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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

