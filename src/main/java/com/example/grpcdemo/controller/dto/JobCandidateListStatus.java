package com.example.grpcdemo.controller.dto;

/**
 * 候选人列表筛选所支持的状态标签。
 */
public enum JobCandidateListStatus {
    /** 全部候选人。 */
    ALL,
    /** 待邀约。 */
    WAITING_INVITE,
    /** 未面试。 */
    NOT_INTERVIEWED,
    /** 已面试。 */
    INTERVIEWED,
    /** 已放弃。 */
    DROPPED
}
