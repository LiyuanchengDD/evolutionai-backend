package com.example.grpcdemo.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * 岗位文档解析详情。
 */
public class JobDocumentResponse {

    private final String documentId;
    private final String fileName;
    private final String fileType;
    private final String parsedTitle;
    private final String parsedLocation;
    private final String parsedPublisher;
    private final Float confidence;
    private final String aiRawResult;
    private final String documentHtml;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant updatedAt;

    public JobDocumentResponse(String documentId,
                               String fileName,
                               String fileType,
                               String parsedTitle,
                               String parsedLocation,
                               String parsedPublisher,
                               Float confidence,
                               String aiRawResult,
                               String documentHtml,
                               Instant createdAt,
                               Instant updatedAt) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.parsedTitle = parsedTitle;
        this.parsedLocation = parsedLocation;
        this.parsedPublisher = parsedPublisher;
        this.confidence = confidence;
        this.aiRawResult = aiRawResult;
        this.documentHtml = documentHtml;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getParsedTitle() {
        return parsedTitle;
    }

    public String getParsedLocation() {
        return parsedLocation;
    }

    public String getParsedPublisher() {
        return parsedPublisher;
    }

    public Float getConfidence() {
        return confidence;
    }

    public String getAiRawResult() {
        return aiRawResult;
    }

    public String getDocumentHtml() {
        return documentHtml;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
