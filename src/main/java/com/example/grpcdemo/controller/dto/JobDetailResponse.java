package com.example.grpcdemo.controller.dto;

import com.example.grpcdemo.entity.RecruitingPositionSource;

/**
 * 岗位详情信息，包括来源与文档解析结果。
 */
public class JobDetailResponse {

    private final JobCardResponse card;
    private final RecruitingPositionSource source;
    private final JobDocumentResponse document;

    public JobDetailResponse(JobCardResponse card, RecruitingPositionSource source, JobDocumentResponse document) {
        this.card = card;
        this.source = source;
        this.document = document;
    }

    public JobCardResponse getCard() {
        return card;
    }

    public RecruitingPositionSource getSource() {
        return source;
    }

    public JobDocumentResponse getDocument() {
        return document;
    }
}
