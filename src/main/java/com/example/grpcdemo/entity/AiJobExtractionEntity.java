package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 职位描述解析历史记录。
 */
@Entity
@Table(name = "ai_job_extractions")
public class AiJobExtractionEntity {

    @Id
    @Column(name = "extraction_id", nullable = false, length = 36)
    private String extractionId;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_type", length = 128)
    private String fileType;

    @Column(name = "extracted_title", length = 255)
    private String extractedTitle;

    @Column(name = "extracted_location", length = 255)
    private String extractedLocation;

    @Lob
    @Column(name = "raw_text")
    private String rawText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getExtractionId() {
        return extractionId;
    }

    public void setExtractionId(String extractionId) {
        this.extractionId = extractionId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getExtractedTitle() {
        return extractedTitle;
    }

    public void setExtractedTitle(String extractedTitle) {
        this.extractedTitle = extractedTitle;
    }

    public String getExtractedLocation() {
        return extractedLocation;
    }

    public void setExtractedLocation(String extractedLocation) {
        this.extractedLocation = extractedLocation;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
