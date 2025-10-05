package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AI 评估写入请求。字段预留给智能体团队扩展。
 */
public class CandidateAiEvaluationRequest {

    @Size(max = 36, message = "评估 ID 长度不能超过 36 字符")
    private String evaluationId;

    @Size(max = 36, message = "面试记录 ID 长度不能超过 36 字符")
    private String interviewRecordId;

    private BigDecimal overallScore;

    @Size(max = 32, message = "评分等级长度不能超过 32 字符")
    private String scoreLevel;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> riskAlerts;

    private List<String> recommendations;

    private Map<String, BigDecimal> competencyScores;

    private Map<String, Object> customMetrics;

    @Size(max = 64, message = "模型版本长度不能超过 64 字符")
    private String aiModelVersion;

    private Instant evaluatedAt;

    private String rawPayload;

    public String getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(String evaluationId) {
        this.evaluationId = evaluationId;
    }

    public String getInterviewRecordId() {
        return interviewRecordId;
    }

    public void setInterviewRecordId(String interviewRecordId) {
        this.interviewRecordId = interviewRecordId;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public String getScoreLevel() {
        return scoreLevel;
    }

    public void setScoreLevel(String scoreLevel) {
        this.scoreLevel = scoreLevel;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getRiskAlerts() {
        return riskAlerts;
    }

    public void setRiskAlerts(List<String> riskAlerts) {
        this.riskAlerts = riskAlerts;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public Map<String, BigDecimal> getCompetencyScores() {
        return competencyScores;
    }

    public void setCompetencyScores(Map<String, BigDecimal> competencyScores) {
        this.competencyScores = competencyScores;
    }

    public Map<String, Object> getCustomMetrics() {
        return customMetrics;
    }

    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = customMetrics;
    }

    public String getAiModelVersion() {
        return aiModelVersion;
    }

    public void setAiModelVersion(String aiModelVersion) {
        this.aiModelVersion = aiModelVersion;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}

