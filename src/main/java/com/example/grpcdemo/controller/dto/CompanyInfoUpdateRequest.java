package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.onboarding.AnnualHiringPlan;
import com.example.grpcdemo.onboarding.EmployeeScale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Payload used when an enterprise admin updates the company level information
 * from the profile page.
 */
public class CompanyInfoUpdateRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String companyName;

    private String companyShortName;

    private String socialCreditCode;

    @NotBlank
    private String country;

    @NotBlank
    private String city;

    @NotNull
    private EmployeeScale employeeScale;

    @NotNull
    private AnnualHiringPlan annualHiringPlan;

    private String industry;

    private String website;

    private String description;

    private String detailedAddress;

    private List<@NotBlank String> recruitingPositions;

    private String contactEmail;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public EmployeeScale getEmployeeScale() {
        return employeeScale;
    }

    public void setEmployeeScale(EmployeeScale employeeScale) {
        this.employeeScale = employeeScale;
    }

    public AnnualHiringPlan getAnnualHiringPlan() {
        return annualHiringPlan;
    }

    public void setAnnualHiringPlan(AnnualHiringPlan annualHiringPlan) {
        this.annualHiringPlan = annualHiringPlan;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
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

    public String getDetailedAddress() {
        return detailedAddress;
    }

    public void setDetailedAddress(String detailedAddress) {
        this.detailedAddress = detailedAddress;
    }

    public List<String> getRecruitingPositions() {
        return recruitingPositions;
    }

    public void setRecruitingPositions(List<String> recruitingPositions) {
        this.recruitingPositions = recruitingPositions;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
