package com.example.grpcdemo.controller.dto;

/**
 * 简历信息提取返回体。
 */
public class ResumeExtractionResponse {

    private String email;
    private String name;
    private String telephone;

    public ResumeExtractionResponse() {
    }

    public ResumeExtractionResponse(String email, String name, String telephone) {
        this.email = email;
        this.name = name;
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}
