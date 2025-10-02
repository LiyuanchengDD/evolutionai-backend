package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.onboarding.AnnualHiringPlan;
import com.example.grpcdemo.onboarding.EmployeeScale;

/**
 * Company information snapshot returned during onboarding progress queries.
 */
public class EnterpriseCompanyInfoDto {

    private String companyName;
    private String companyShortName;
    private String socialCreditCode;
    private String country;
    private String city;
    private String countryDisplayName;
    private String cityDisplayName;
    private EmployeeScale employeeScale;
    private AnnualHiringPlan annualHiringPlan;
    private String industry;
    private String website;
    private String description;

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

    public String getCountryDisplayName() {
        return countryDisplayName;
    }

    public void setCountryDisplayName(String countryDisplayName) {
        this.countryDisplayName = countryDisplayName;
    }

    public String getCityDisplayName() {
        return cityDisplayName;
    }

    public void setCityDisplayName(String cityDisplayName) {
        this.cityDisplayName = cityDisplayName;
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
}
