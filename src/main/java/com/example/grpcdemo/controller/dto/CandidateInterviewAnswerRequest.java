package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 候选人提交单题作答时的载荷。
 */
public class CandidateInterviewAnswerRequest {

    @NotNull(message = "题目序号不能为空")
    @Min(value = 1, message = "题目序号必须从 1 开始")
    @Max(value = 999, message = "题目序号不能超过 999")
    private Integer questionSequence;

    @Size(max = 36, message = "音频 ID 长度不能超过 36 字符")
    private String audioId;

    @Size(max = 255, message = "文件名长度不能超过 255 字符")
    private String fileName;

    @Size(max = 100, message = "音频类型长度不能超过 100 字符")
    private String contentType;

    @NotBlank(message = "音频内容不能为空")
    private String base64Content;

    @Min(value = 1, message = "答题时长必须大于 0")
    @Max(value = 3600, message = "答题时长不能超过 3600 秒")
    private Integer durationSeconds;

    @Size(max = 5000, message = "转写文本长度不能超过 5000 字符")
    private String transcript;

    private Map<String, Object> metadata;

    public Integer getQuestionSequence() {
        return questionSequence;
    }

    public void setQuestionSequence(Integer questionSequence) {
        this.questionSequence = questionSequence;
    }

    public String getAudioId() {
        return audioId;
    }

    public void setAudioId(String audioId) {
        this.audioId = audioId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

