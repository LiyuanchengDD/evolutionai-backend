package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * AI 面试评估结果实体，占位字段方便后续扩展。
 */
@Entity
@Table(name = "candidate_ai_evaluations")
public class CandidateAiEvaluationEntity {

    @Id
    @Column(name = "evaluation_id", nullable = false, length = 36)
    private String evaluationId;

    @Column(name = "job_candidate_id", nullable = false, length = 36)
    private String jobCandidateId;

    @Column(name = "interview_record_id", length = 36)
    private String interviewRecordId;

    @Column(name = "ai_model_version", length = 64)
    private String aiModelVersion;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "score_level", length = 32)
    private String scoreLevel;

    @Lob
    @Column(name = "strengths_json")
    private String strengthsJson;

    @Lob
    @Column(name = "weaknesses_json")
    private String weaknessesJson;

    @Lob
    @Column(name = "risk_alerts_json")
    private String riskAlertsJson;

    @Lob
    @Column(name = "recommendations_json")
    private String recommendationsJson;

    @Lob
    @Column(name = "competency_scores_json")
    private String competencyScoresJson;

    @Lob
    @Column(name = "custom_metrics_json")
    private String customMetricsJson;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

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

    public String getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(String evaluationId) {
        this.evaluationId = evaluationId;
    }

    public String getJobCandidateId() {
        return jobCandidateId;
    }

    public void setJobCandidateId(String jobCandidateId) {
        this.jobCandidateId = jobCandidateId;
    }

    public String getInterviewRecordId() {
        return interviewRecordId;
    }

    public void setInterviewRecordId(String interviewRecordId) {
        this.interviewRecordId = interviewRecordId;
    }

    public String getAiModelVersion() {
        return aiModelVersion;
    }

    public void setAiModelVersion(String aiModelVersion) {
        this.aiModelVersion = aiModelVersion;
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

    public String getStrengthsJson() {
        return strengthsJson;
    }

    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    public String getWeaknessesJson() {
        return weaknessesJson;
    }

    public void setWeaknessesJson(String weaknessesJson) {
        this.weaknessesJson = weaknessesJson;
    }

    public String getRiskAlertsJson() {
        return riskAlertsJson;
    }

    public void setRiskAlertsJson(String riskAlertsJson) {
        this.riskAlertsJson = riskAlertsJson;
    }

    public String getRecommendationsJson() {
        return recommendationsJson;
    }

    public void setRecommendationsJson(String recommendationsJson) {
        this.recommendationsJson = recommendationsJson;
    }

    public String getCompetencyScoresJson() {
        return competencyScoresJson;
    }

    public void setCompetencyScoresJson(String competencyScoresJson) {
        this.competencyScoresJson = competencyScoresJson;
    }

    public String getCustomMetricsJson() {
        return customMetricsJson;
    }

    public void setCustomMetricsJson(String customMetricsJson) {
        this.customMetricsJson = customMetricsJson;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
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

