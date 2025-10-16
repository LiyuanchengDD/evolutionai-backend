package com.example.grpcdemo.controller.dto;

public class TrialApplicationAdminResponse {

    private final String id;
    private final String applicantUserId;
    private final String companyName;
    private final String contactEmail;
    private final String reason;
    private final String status;
    private final String reviewedBy;
    private final String reviewedAt;
    private final String note;
    private final String createdAt;

    public TrialApplicationAdminResponse(String id,
                                         String applicantUserId,
                                         String companyName,
                                         String contactEmail,
                                         String reason,
                                         String status,
                                         String reviewedBy,
                                         String reviewedAt,
                                         String note,
                                         String createdAt) {
        this.id = id;
        this.applicantUserId = applicantUserId;
        this.companyName = companyName;
        this.contactEmail = contactEmail;
        this.reason = reason;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.note = note;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getApplicantUserId() {
        return applicantUserId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public String getNote() {
        return note;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
