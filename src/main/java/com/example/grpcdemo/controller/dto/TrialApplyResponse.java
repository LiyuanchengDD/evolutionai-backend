package com.example.grpcdemo.controller.dto;

public class TrialApplyResponse {

    private final String id;
    private final String status;

    public TrialApplyResponse(String id, String status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }
}
