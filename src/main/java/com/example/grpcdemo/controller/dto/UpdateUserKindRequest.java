package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateUserKindRequest {

    @NotBlank
    private String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
