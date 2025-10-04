package com.example.grpcdemo.entity;

/**
 * 邀约状态。
 */
public enum JobCandidateInviteStatus {
    /** 待邀约（已解析出邮箱）。 */
    INVITE_PENDING,
    /** 已发送邀约邮件。 */
    INVITE_SENT,
    /** 邮件发送失败。 */
    INVITE_FAILED,
    /** 邮箱缺失或无效，无法发送邀约。 */
    EMAIL_MISSING
}
