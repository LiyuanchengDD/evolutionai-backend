package com.example.grpcdemo.service;

import java.util.Arrays;
import java.util.Objects;

/**
 * 简历解析指令。
 */
public final class ResumeParsingCommand {

    private final byte[] fileContent;
    private final String fileName;
    private final String contentType;
    private final String companyId;
    private final String positionId;
    private final String uploaderUserId;

    public ResumeParsingCommand(byte[] fileContent,
                                String fileName,
                                String contentType,
                                String companyId,
                                String positionId,
                                String uploaderUserId) {
        this.fileContent = Objects.requireNonNull(fileContent, "fileContent");
        this.fileName = fileName;
        this.contentType = contentType;
        this.companyId = companyId;
        this.positionId = positionId;
        this.uploaderUserId = uploaderUserId;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getPositionId() {
        return positionId;
    }

    public String getUploaderUserId() {
        return uploaderUserId;
    }

    @Override
    public String toString() {
        return "ResumeParsingCommand{" +
                "fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", companyId='" + companyId + '\'' +
                ", positionId='" + positionId + '\'' +
                ", uploaderUserId='" + uploaderUserId + '\'' +
                ", fileContentLength=" + (fileContent != null ? fileContent.length : 0) +
                '}';
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileName, contentType, companyId, positionId, uploaderUserId);
        result = 31 * result + Arrays.hashCode(fileContent);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ResumeParsingCommand that = (ResumeParsingCommand) obj;
        return Arrays.equals(fileContent, that.fileContent)
                && Objects.equals(fileName, that.fileName)
                && Objects.equals(contentType, that.contentType)
                && Objects.equals(companyId, that.companyId)
                && Objects.equals(positionId, that.positionId)
                && Objects.equals(uploaderUserId, that.uploaderUserId);
    }
}
