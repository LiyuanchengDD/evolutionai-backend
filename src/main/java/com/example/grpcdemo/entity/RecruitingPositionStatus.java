package com.example.grpcdemo.entity;

/**
 * 状态枚举描述岗位在导入与发布过程中的阶段。
 */
public enum RecruitingPositionStatus {

    /**
     * 刚由 AI 解析生成的草稿，尚待 HR 校验。
     */
    DRAFT_PARSED,

    /**
     * HR 已经确认主要字段，等待发布或进一步操作。
     */
    READY,

    /**
     * 岗位已经正式发布。
     */
    PUBLISHED,

    /**
     * 岗位已关闭，不再展示给候选人。
     */
    CLOSED,

    /**
     * AI 解析失败，需要人工完全录入。
     */
    PARSE_FAILED
}
