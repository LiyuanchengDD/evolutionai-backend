package com.example.grpcdemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 存储岗位导入时上传的 PDF 文档以及 AI 解析结果的快照。
 */
@Entity
@Table(name = "company_job_documents")
public class CompanyJobDocumentEntity {

    @Id
    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "position_id", nullable = false, length = 36)
    private String positionId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "storage_bucket", length = 128)
    private String storageBucket;

    @Column(name = "storage_path", length = 512)
    private String storagePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "upload_user_id", length = 36)
    private String uploadUserId;

    @Lob
    @Column(name = "ai_raw_result")
    private String aiRawResult;

    @Column(name = "parsed_title", length = 255)
    private String parsedTitle;

    @Column(name = "parsed_location", length = 255)
    private String parsedLocation;

    @Column(name = "parsed_publisher", length = 255)
    private String parsedPublisher;

    @Column(name = "confidence")
    private Float confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(String uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public String getAiRawResult() {
        return aiRawResult;
    }

    public void setAiRawResult(String aiRawResult) {
        this.aiRawResult = aiRawResult;
    }

    public String getParsedTitle() {
        return parsedTitle;
    }

    public void setParsedTitle(String parsedTitle) {
        this.parsedTitle = parsedTitle;
    }

    public String getParsedLocation() {
        return parsedLocation;
    }

    public void setParsedLocation(String parsedLocation) {
        this.parsedLocation = parsedLocation;
    }

    public String getParsedPublisher() {
        return parsedPublisher;
    }

    public void setParsedPublisher(String parsedPublisher) {
        this.parsedPublisher = parsedPublisher;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
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
