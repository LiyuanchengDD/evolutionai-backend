package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Email;

import java.util.ArrayList;
import java.util.List;

/**
 * 发送邀约请求。
 */
public class JobCandidateInviteRequest {

    private String templateId;

    private List<@Email(message = "抄送邮箱格式不正确") String> cc = new ArrayList<>();

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }
}
