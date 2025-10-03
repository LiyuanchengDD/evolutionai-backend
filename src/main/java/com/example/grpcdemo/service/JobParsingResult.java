package com.example.grpcdemo.service;

import java.util.Objects;

/**
 * 表示 AI 解析返回的结构化字段。
 */
public final class JobParsingResult {

    private final String title;
    private final String location;
    private final String publisherNickname;
    private final Float confidence;
    private final String rawJson;

    public JobParsingResult(String title, String location, String publisherNickname, Float confidence, String rawJson) {
        this.title = title;
        this.location = location;
        this.publisherNickname = publisherNickname;
        this.confidence = confidence;
        this.rawJson = rawJson;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getPublisherNickname() {
        return publisherNickname;
    }

    public Float getConfidence() {
        return confidence;
    }

    public String getRawJson() {
        return rawJson;
    }

    public boolean hasStructuredFields() {
        return title != null || location != null || publisherNickname != null;
    }

    @Override
    public String toString() {
        return "JobParsingResult{" +
                "title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", publisherNickname='" + publisherNickname + '\'' +
                ", confidence=" + confidence +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobParsingResult that = (JobParsingResult) o;
        return Objects.equals(title, that.title)
                && Objects.equals(location, that.location)
                && Objects.equals(publisherNickname, that.publisherNickname)
                && Objects.equals(confidence, that.confidence)
                && Objects.equals(rawJson, that.rawJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, location, publisherNickname, confidence, rawJson);
    }
}
