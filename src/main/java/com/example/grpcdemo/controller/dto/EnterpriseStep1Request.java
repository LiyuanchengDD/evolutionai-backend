package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.onboarding.EmployeeScale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for enterprise onboarding step 1 - company profile basics.
 */
public class EnterpriseStep1Request {

    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    @NotBlank(message = "企业全称不能为空")
    private String companyName;

    @Size(max = 255, message = "企业简称长度需小于 255 个字符")
    private String companyShortName;

    @Size(max = 64, message = "统一社会信用代码长度需小于 64 个字符")
    private String socialCreditCode;

    @NotBlank(message = "所属国家不能为空")
    private String country;

    @NotBlank(message = "所属城市不能为空")
    private String city;

    @NotNull(message = "请选择企业规模")
    private EmployeeScale employeeScale;

    @Size(max = 255, message = "行业字段长度需小于 255 个字符")
    private String industry;

    @Size(max = 255, message = "企业官网长度需小于 255 个字符")
    private String website;

    @Size(max = 1000, message = "企业简介长度需小于 1000 个字符")
    private String description;

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
