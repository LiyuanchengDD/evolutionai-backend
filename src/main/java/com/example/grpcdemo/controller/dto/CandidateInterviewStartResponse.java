package com.example.grpcdemo.controller.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 返回给前端的面试启动信息。
 */
public class CandidateInterviewStartResponse {

    private String interviewRecordId;
    private String aiSessionId;
    private Instant interviewStartedAt;
    private Instant answerDeadlineAt;
    private List<CandidateInterviewQuestionDto> questions;
    private Map<String, Object> metadata;

    public String getInterviewRecordId() {
        return interviewRecordId;
    }

    public void setInterviewRecordId(String interviewRecordId) {
        this.interviewRecordId = interviewRecordId;
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

    public Instant getAnswerDeadlineAt() {
        return answerDeadlineAt;
    }

    public void setAnswerDeadlineAt(Instant answerDeadlineAt) {
        this.answerDeadlineAt = answerDeadlineAt;
    }

    public List<CandidateInterviewQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<CandidateInterviewQuestionDto> questions) {
        this.questions = questions;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

