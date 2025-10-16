package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TrialReviewRequest {

    @NotNull(message = "审批结果不能为空")
    private Boolean approve;

    @Size(max = 1000, message = "备注过长")
    private String note;

    public Boolean getApprove() {
        return approve;
    }

    public void setApprove(Boolean approve) {
        this.approve = approve;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
