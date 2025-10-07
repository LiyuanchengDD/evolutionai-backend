package com.example.grpcdemo.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AI 面试记录写入请求。
 */
public class CandidateInterviewRecordRequest {

    @Size(max = 36, message = "面试记录 ID 长度不能超过 36 字符")
    private String interviewRecordId;

    @Size(max = 64, message = "AI 会话 ID 长度不能超过 64 字符")
    private String aiSessionId;

    @Size(max = 32, message = "面试方式长度不能超过 32 字符")
    private String interviewMode;

    @Size(max = 255, message = "面试官名称长度不能超过 255 字符")
    private String interviewerName;

    private Instant interviewStartedAt;

    private Instant interviewEndedAt;

    private Integer durationSeconds;

    @Valid
    private List<CandidateInterviewQuestionDto> questions;

    @Valid
    private List<CandidateInterviewAudioRequest> audios;

    private Integer currentQuestionSequence;

    /**
     * 面试前硬件检测结果。
     */
    @Valid
    private CandidateInterviewPrecheckDto precheck;

    /**
     * 头像上传请求，若 `remove=true` 表示清空头像。
     */
    @Valid
    private CandidateInterviewProfilePhotoRequest profilePhoto;

    /**
     * 对话/语音转写等完整原始数据，JSON 字符串。
     */
    private String transcriptRaw;

    /**
     * 供前端展示的额外元数据（例如答题评分、题目分类等）。
     */
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

    public List<CandidateInterviewAudioRequest> getAudios() {
        return audios;
    }

    public void setAudios(List<CandidateInterviewAudioRequest> audios) {
        this.audios = audios;
    }

    public Integer getCurrentQuestionSequence() {
        return currentQuestionSequence;
    }

    public void setCurrentQuestionSequence(Integer currentQuestionSequence) {
        this.currentQuestionSequence = currentQuestionSequence;
    }

    public CandidateInterviewPrecheckDto getPrecheck() {
        return precheck;
    }

    public void setPrecheck(CandidateInterviewPrecheckDto precheck) {
        this.precheck = precheck;
    }

    public CandidateInterviewProfilePhotoRequest getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(CandidateInterviewProfilePhotoRequest profilePhoto) {
        this.profilePhoto = profilePhoto;
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
}

