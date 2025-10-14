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
 * 面试记录实体，保存候选人在智能面试中的问答轨迹。
 */
@Entity
@Table(name = "candidate_interview_records")
public class CandidateInterviewRecordEntity {

    @Id
    @Column(name = "record_id", nullable = false, length = 36)
    private String recordId;

    @Column(name = "job_candidate_id", nullable = false, length = 36)
    private String jobCandidateId;

    @Column(name = "interview_mode", length = 32)
    private String interviewMode;

    @Column(name = "interviewer_name", length = 255)
    private String interviewerName;

    @Column(name = "ai_session_id", length = 64)
    private String aiSessionId;

    @Column(name = "precheck_status", length = 32)
    private String precheckStatus;

    @Lob
    @Column(name = "precheck_report_json")
    private String precheckReportJson;

    @Column(name = "precheck_completed_at")
    private Instant precheckCompletedAt;

    @Column(name = "room_entered_at")
    private Instant roomEnteredAt;

    @Column(name = "interview_started_at")
    private Instant interviewStartedAt;

    @Column(name = "interview_ended_at")
    private Instant interviewEndedAt;

    @Column(name = "answer_deadline_at")
    private Instant answerDeadlineAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "answer_elapsed_seconds")
    private Integer answerElapsedSeconds;

    @Column(name = "answer_resumed_at")
    private Instant answerResumedAt;

    @Column(name = "answer_paused_at")
    private Instant answerPausedAt;

    @Column(name = "current_question_sequence")
    private Integer currentQuestionSequence;

    @Lob
    @Column(name = "questions_json")
    private String questionsJson;

    @Lob
    @Column(name = "transcript_json")
    private String transcriptJson;

    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "profile_photo_storage_bucket", length = 128)
    private String profilePhotoStorageBucket;

    @Column(name = "profile_photo_storage_path", length = 512)
    private String profilePhotoStoragePath;

    @Column(name = "profile_photo_file_name", length = 255)
    private String profilePhotoFileName;

    @Column(name = "profile_photo_content_type", length = 100)
    private String profilePhotoContentType;

    @Column(name = "profile_photo_size_bytes")
    private Long profilePhotoSizeBytes;

    @Column(name = "profile_photo_uploaded_at")
    private Instant profilePhotoUploadedAt;

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

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
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

    public String getPrecheckStatus() {
        return precheckStatus;
    }

    public void setPrecheckStatus(String precheckStatus) {
        this.precheckStatus = precheckStatus;
    }

    public String getPrecheckReportJson() {
        return precheckReportJson;
    }

    public void setPrecheckReportJson(String precheckReportJson) {
        this.precheckReportJson = precheckReportJson;
    }

    public Instant getPrecheckCompletedAt() {
        return precheckCompletedAt;
    }

    public void setPrecheckCompletedAt(Instant precheckCompletedAt) {
        this.precheckCompletedAt = precheckCompletedAt;
    }

    public Instant getRoomEnteredAt() {
        return roomEnteredAt;
    }

    public void setRoomEnteredAt(Instant roomEnteredAt) {
        this.roomEnteredAt = roomEnteredAt;
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

    public Instant getAnswerDeadlineAt() {
        return answerDeadlineAt;
    }

    public void setAnswerDeadlineAt(Instant answerDeadlineAt) {
        this.answerDeadlineAt = answerDeadlineAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getAnswerElapsedSeconds() {
        return answerElapsedSeconds;
    }

    public void setAnswerElapsedSeconds(Integer answerElapsedSeconds) {
        this.answerElapsedSeconds = answerElapsedSeconds;
    }

    public Instant getAnswerResumedAt() {
        return answerResumedAt;
    }

    public void setAnswerResumedAt(Instant answerResumedAt) {
        this.answerResumedAt = answerResumedAt;
    }

    public Instant getAnswerPausedAt() {
        return answerPausedAt;
    }

    public void setAnswerPausedAt(Instant answerPausedAt) {
        this.answerPausedAt = answerPausedAt;
    }

    public Integer getCurrentQuestionSequence() {
        return currentQuestionSequence;
    }

    public void setCurrentQuestionSequence(Integer currentQuestionSequence) {
        this.currentQuestionSequence = currentQuestionSequence;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public String getTranscriptJson() {
        return transcriptJson;
    }

    public void setTranscriptJson(String transcriptJson) {
        this.transcriptJson = transcriptJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getProfilePhotoStorageBucket() {
        return profilePhotoStorageBucket;
    }

    public void setProfilePhotoStorageBucket(String profilePhotoStorageBucket) {
        this.profilePhotoStorageBucket = profilePhotoStorageBucket;
    }

    public String getProfilePhotoStoragePath() {
        return profilePhotoStoragePath;
    }

    public void setProfilePhotoStoragePath(String profilePhotoStoragePath) {
        this.profilePhotoStoragePath = profilePhotoStoragePath;
    }

    public String getProfilePhotoFileName() {
        return profilePhotoFileName;
    }

    public void setProfilePhotoFileName(String profilePhotoFileName) {
        this.profilePhotoFileName = profilePhotoFileName;
    }

    public String getProfilePhotoContentType() {
        return profilePhotoContentType;
    }

    public void setProfilePhotoContentType(String profilePhotoContentType) {
        this.profilePhotoContentType = profilePhotoContentType;
    }

    public Long getProfilePhotoSizeBytes() {
        return profilePhotoSizeBytes;
    }

    public void setProfilePhotoSizeBytes(Long profilePhotoSizeBytes) {
        this.profilePhotoSizeBytes = profilePhotoSizeBytes;
    }

    public Instant getProfilePhotoUploadedAt() {
        return profilePhotoUploadedAt;
    }

    public void setProfilePhotoUploadedAt(Instant profilePhotoUploadedAt) {
        this.profilePhotoUploadedAt = profilePhotoUploadedAt;
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

