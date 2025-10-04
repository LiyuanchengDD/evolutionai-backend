package com.example.grpcdemo.service;

import java.util.Objects;

/**
 * 简历解析结果。
 */
public final class ResumeParsingResult {

    private final String name;
    private final String email;
    private final String phone;
    private final String htmlContent;
    private final Float confidence;
    private final String rawJson;

    public ResumeParsingResult(String name,
                               String email,
                               String phone,
                               String htmlContent,
                               Float confidence,
                               String rawJson) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.htmlContent = htmlContent;
        this.confidence = confidence;
        this.rawJson = rawJson;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public Float getConfidence() {
        return confidence;
    }

    public String getRawJson() {
        return rawJson;
    }

    public boolean hasContact() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }

    @Override
    public String toString() {
        return "ResumeParsingResult{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", confidence=" + confidence +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResumeParsingResult that = (ResumeParsingResult) o;
        return Objects.equals(name, that.name)
                && Objects.equals(email, that.email)
                && Objects.equals(phone, that.phone)
                && Objects.equals(htmlContent, that.htmlContent)
                && Objects.equals(confidence, that.confidence)
                && Objects.equals(rawJson, that.rawJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, phone, htmlContent, confidence, rawJson);
    }
}
