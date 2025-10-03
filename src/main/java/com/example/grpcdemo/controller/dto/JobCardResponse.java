package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.entity.RecruitingPositionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * 岗位卡片摘要信息。
 */
public class JobCardResponse {

    private final String positionId;
    private final String companyId;
    private final String positionName;
    private final String positionLocation;
    private final String publisherNickname;
    private final RecruitingPositionStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant updatedAt;

    public JobCardResponse(String positionId,
                           String companyId,
                           String positionName,
                           String positionLocation,
                           String publisherNickname,
                           RecruitingPositionStatus status,
                           Instant updatedAt) {
        this.positionId = positionId;
        this.companyId = companyId;
        this.positionName = positionName;
        this.positionLocation = positionLocation;
        this.publisherNickname = publisherNickname;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getPositionId() {
        return positionId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getPositionName() {
        return positionName;
    }

    public String getPositionLocation() {
        return positionLocation;
    }

    public String getPublisherNickname() {
        return publisherNickname;
    }

    public RecruitingPositionStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
