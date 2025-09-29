package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for enterprise onboarding step 2 - HR contact info.
 */
public class EnterpriseStep2Request {

    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系人邮箱不能为空")
    @Email(message = "联系人邮箱格式不正确")
    private String contactEmail;

    @NotBlank(message = "区号不能为空")
    private String phoneCountryCode;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^[0-9+\-]{4,32}$", message = "手机号格式不正确")
    private String phoneNumber;

    @Size(max = 128, message = "职位名称长度需小于 128 个字符")
    private String position;

    @Size(max = 128, message = "所属部门长度需小于 128 个字符")
    private String department;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public void setPhoneCountryCode(String phoneCountryCode) {
        this.phoneCountryCode = phoneCountryCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
