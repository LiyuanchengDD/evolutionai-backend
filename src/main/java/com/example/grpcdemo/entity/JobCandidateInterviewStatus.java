package com.example.grpcdemo.entity;

/**
 * 面试状态枚举。
 */
public enum JobCandidateInterviewStatus {
    /** 未安排或未开始面试。 */
    NOT_INTERVIEWED,
    /** 已安排面试，尚未完成。 */
    SCHEDULED,
    /** 面试进行中。 */
    IN_PROGRESS,
    /** 面试已完成。 */
    COMPLETED,
    /** 面试已取消或候选人放弃。 */
    CANCELLED
}
