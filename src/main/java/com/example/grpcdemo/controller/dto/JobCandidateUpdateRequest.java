package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 候选人信息更新请求。
 */
public class JobCandidateUpdateRequest {

    @Size(max = 255, message = "姓名长度不能超过 255 字符")
    private String name;

    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过 255 字符")
    private String email;

    @Size(max = 64, message = "电话长度不能超过 64 字符")
    private String phone;

    private JobCandidateInviteStatus inviteStatus;

    private JobCandidateInterviewStatus interviewStatus;

    private String resumeHtml;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public JobCandidateInviteStatus getInviteStatus() {
        return inviteStatus;
    }

    public void setInviteStatus(JobCandidateInviteStatus inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public JobCandidateInterviewStatus getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(JobCandidateInterviewStatus interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public String getResumeHtml() {
        return resumeHtml;
    }

    public void setResumeHtml(String resumeHtml) {
        this.resumeHtml = resumeHtml;
    }
}
