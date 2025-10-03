package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.entity.RecruitingPositionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;

/**
 * 编辑岗位卡片时提交的字段。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobUpdateRequest {

    @Size(max = 255, message = "岗位名称长度不能超过255个字符")
    private String positionName;

    @Size(max = 255, message = "地点长度不能超过255个字符")
    private String positionLocation;

    @Size(max = 255, message = "发布人昵称长度不能超过255个字符")
    private String publisherNickname;

    private RecruitingPositionStatus status;

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getPositionLocation() {
        return positionLocation;
    }

    public void setPositionLocation(String positionLocation) {
        this.positionLocation = positionLocation;
    }

    public String getPublisherNickname() {
        return publisherNickname;
    }

    public void setPublisherNickname(String publisherNickname) {
        this.publisherNickname = publisherNickname;
    }

    public RecruitingPositionStatus getStatus() {
        return status;
    }

    public void setStatus(RecruitingPositionStatus status) {
        this.status = status;
    }
}
