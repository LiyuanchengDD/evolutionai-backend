package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;

import java.time.Instant;

/**
 * 候选人在面试门户左侧列表展示的邀约卡片。
 */
public class CandidateInterviewInvitationItem {

    private String jobCandidateId;
    private String positionId;
    private String positionName;
    private String companyId;
    private String companyName;
    private String countryCode;
    private String countryName;
    private String cityCode;
    private String cityName;
    private String hrName;
    private String hrEmail;
    private String hrPhone;
    private JobCandidateInterviewStatus interviewStatus;
    private JobCandidateInviteStatus inviteStatus;
    private Instant lastInviteSentAt;
    private Instant interviewDeadlineAt;
    private Instant interviewCompletedAt;
    private Instant updatedAt;
    private Boolean precheckPassed;
    private Boolean profilePhotoUploaded;
    private Instant answerDeadlineAt;
    private String candidateEmail;
    private CandidateInterviewProfilePhotoDto profilePhoto;
    private String failureReason;

    public String getJobCandidateId() {
        return jobCandidateId;
    }

    public void setJobCandidateId(String jobCandidateId) {
        this.jobCandidateId = jobCandidateId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getHrName() {
        return hrName;
    }

    public void setHrName(String hrName) {
        this.hrName = hrName;
    }

    public String getHrEmail() {
        return hrEmail;
    }

    public void setHrEmail(String hrEmail) {
        this.hrEmail = hrEmail;
    }

    public String getHrPhone() {
        return hrPhone;
    }

    public void setHrPhone(String hrPhone) {
        this.hrPhone = hrPhone;
    }

    public JobCandidateInterviewStatus getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(JobCandidateInterviewStatus interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public JobCandidateInviteStatus getInviteStatus() {
        return inviteStatus;
    }

    public void setInviteStatus(JobCandidateInviteStatus inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public Instant getLastInviteSentAt() {
        return lastInviteSentAt;
    }

    public void setLastInviteSentAt(Instant lastInviteSentAt) {
        this.lastInviteSentAt = lastInviteSentAt;
    }

    public Instant getInterviewDeadlineAt() {
        return interviewDeadlineAt;
    }

    public void setInterviewDeadlineAt(Instant interviewDeadlineAt) {
        this.interviewDeadlineAt = interviewDeadlineAt;
    }

    public Instant getInterviewCompletedAt() {
        return interviewCompletedAt;
    }

    public void setInterviewCompletedAt(Instant interviewCompletedAt) {
        this.interviewCompletedAt = interviewCompletedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getPrecheckPassed() {
        return precheckPassed;
    }

    public void setPrecheckPassed(Boolean precheckPassed) {
        this.precheckPassed = precheckPassed;
    }

    public Boolean getProfilePhotoUploaded() {
        return profilePhotoUploaded;
    }

    public void setProfilePhotoUploaded(Boolean profilePhotoUploaded) {
        this.profilePhotoUploaded = profilePhotoUploaded;
    }

    public Instant getAnswerDeadlineAt() {
        return answerDeadlineAt;
    }

    public void setAnswerDeadlineAt(Instant answerDeadlineAt) {
        this.answerDeadlineAt = answerDeadlineAt;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public CandidateInterviewProfilePhotoDto getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(CandidateInterviewProfilePhotoDto profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}

