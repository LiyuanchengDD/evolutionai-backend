package com.example.grpcdemo.controller.dto;

/**
 * JD 信息提取返回体。
 */
public class JobExtractionResponse {

    private String location;
    private String title;

    public JobExtractionResponse() {
    }

    public JobExtractionResponse(String location, String title) {
        this.location = location;
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
