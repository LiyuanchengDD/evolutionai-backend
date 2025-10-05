package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 面试问答详情项，供前端还原问答列表。
 */
public class CandidateInterviewQuestionDto {

    @NotNull(message = "题目序号不能为空")
    private Integer sequence;

    @Size(max = 255, message = "题目标题长度不能超过 255 字符")
    private String questionTitle;

    private String questionDescription;

    private String candidateAnswer;

    private String answerMediaUrl;

    private Integer answerDurationSeconds;

    private Integer questionScore;

    private String aiFeedback;

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public String getQuestionDescription() {
        return questionDescription;
    }

    public void setQuestionDescription(String questionDescription) {
        this.questionDescription = questionDescription;
    }

    public String getCandidateAnswer() {
        return candidateAnswer;
    }

    public void setCandidateAnswer(String candidateAnswer) {
        this.candidateAnswer = candidateAnswer;
    }

    public String getAnswerMediaUrl() {
        return answerMediaUrl;
    }

    public void setAnswerMediaUrl(String answerMediaUrl) {
        this.answerMediaUrl = answerMediaUrl;
    }

    public Integer getAnswerDurationSeconds() {
        return answerDurationSeconds;
    }

    public void setAnswerDurationSeconds(Integer answerDurationSeconds) {
        this.answerDurationSeconds = answerDurationSeconds;
    }

    public Integer getQuestionScore() {
        return questionScore;
    }

    public void setQuestionScore(Integer questionScore) {
        this.questionScore = questionScore;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }
}

