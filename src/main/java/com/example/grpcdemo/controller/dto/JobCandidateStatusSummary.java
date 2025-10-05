package com.example.grpcdemo.controller.dto;

/**
 * 岗位候选人状态聚合。
 */
public class JobCandidateStatusSummary {

    /** 待邀约（包含邮箱缺失、邀约失败等情况）。 */
    private long waitingInvite;
    /** 未进行面试（包含已安排但未完成）。 */
    private long notInterviewed;
    /** 已完成面试。 */
    private long interviewed;
    /** 已放弃或已取消。 */
    private long dropped;
    /** 全部候选人数量。 */
    private long all;

    public long getWaitingInvite() {
        return waitingInvite;
    }

    public void setWaitingInvite(long waitingInvite) {
        this.waitingInvite = waitingInvite;
    }

    public long getNotInterviewed() {
        return notInterviewed;
    }

    public void setNotInterviewed(long notInterviewed) {
        this.notInterviewed = notInterviewed;
    }

    public long getInterviewed() {
        return interviewed;
    }

    public void setInterviewed(long interviewed) {
        this.interviewed = interviewed;
    }

    public long getDropped() {
        return dropped;
    }

    public void setDropped(long dropped) {
        this.dropped = dropped;
    }

    public long getAll() {
        return all;
    }

    public void setAll(long all) {
        this.all = all;
    }
}
