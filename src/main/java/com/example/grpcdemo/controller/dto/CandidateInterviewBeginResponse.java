package com.example.grpcdemo.controller.dto;

import java.time.Instant;

/**
 * 候选人点击“开始录音”后返回的时间信息。
 */
public class CandidateInterviewBeginResponse {

    private String interviewRecordId;
    private Instant interviewStartedAt;
    private Instant answerDeadlineAt;

    public String getInterviewRecordId() {
        return interviewRecordId;
    }

    public void setInterviewRecordId(String interviewRecordId) {
        this.interviewRecordId = interviewRecordId;
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
}
