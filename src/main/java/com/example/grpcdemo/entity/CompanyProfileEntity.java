package com.example.grpcdemo.entity;

import com.example.grpcdemo.onboarding.CompanyStatus;
import com.example.grpcdemo.onboarding.EmployeeScale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Enterprise company profile persisted after onboarding completes.
 */
@Entity
@Table(name = "company_profiles")
public class CompanyProfileEntity {

    @Id
    @Column(name = "company_id", nullable = false, length = 36)
    private String companyId;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "company_short_name", length = 255)
    private String companyShortName;

    @Column(name = "social_credit_code", length = 64)
    private String socialCreditCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_scale", nullable = false, length = 64)
    private EmployeeScale employeeScale;

    @Column(name = "industry", length = 255)
    private String industry;

    @Column(name = "country", nullable = false, length = 128)
    private String country;

    @Column(name = "city", nullable = false, length = 128)
    private String city;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CompanyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyShortName() {
        return companyShortName;
    }

    public void setCompanyShortName(String companyShortName) {
        this.companyShortName = companyShortName;
    }

    public String getSocialCreditCode() {
        return socialCreditCode;
    }

    public void setSocialCreditCode(String socialCreditCode) {
        this.socialCreditCode = socialCreditCode;
    }

    public EmployeeScale getEmployeeScale() {
        return employeeScale;
    }

    public void setEmployeeScale(EmployeeScale employeeScale) {
        this.employeeScale = employeeScale;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyStatus status) {
        this.status = status;
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
