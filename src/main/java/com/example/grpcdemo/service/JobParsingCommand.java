package com.example.grpcdemo.service;

import java.util.Arrays;
import java.util.Objects;

/**
 * 封装一次岗位解析所需的全部上下文。
 */
public final class JobParsingCommand {

    private final byte[] fileContent;
    private final String fileName;
    private final String contentType;
    private final String companyId;
    private final String uploaderUserId;

    public JobParsingCommand(byte[] fileContent, String fileName, String contentType, String companyId, String uploaderUserId) {
        this.fileContent = Objects.requireNonNull(fileContent, "fileContent");
        this.fileName = fileName;
        this.contentType = contentType;
        this.companyId = companyId;
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

    public String getUploaderUserId() {
        return uploaderUserId;
    }

    @Override
    public String toString() {
        return "JobParsingCommand{" +
                "fileContent=" + fileContent.length +
                ", fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", companyId='" + companyId + '\'' +
                ", uploaderUserId='" + uploaderUserId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobParsingCommand that = (JobParsingCommand) o;
        return Arrays.equals(fileContent, that.fileContent)
                && Objects.equals(fileName, that.fileName)
                && Objects.equals(contentType, that.contentType)
                && Objects.equals(companyId, that.companyId)
                && Objects.equals(uploaderUserId, that.uploaderUserId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileName, contentType, companyId, uploaderUserId);
        result = 31 * result + Arrays.hashCode(fileContent);
        return result;
    }
}
