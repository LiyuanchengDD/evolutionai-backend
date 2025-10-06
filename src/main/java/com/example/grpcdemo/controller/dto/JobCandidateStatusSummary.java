package com.example.grpcdemo.controller.dto;

import java.util.Map;

/**
 * 岗位候选人状态聚合。
 */
public class JobCandidateStatusSummary {

    /** 待邀约（包含邮箱缺失、邀约失败等情况）。 */
    private long waitingInvite;
    /** 已成功发出邀约。 */
    private long invited;
    /** 未进行面试（包含已安排但未完成）。 */
    private long notInterviewed;
    /** 已完成面试。 */
    private long interviewed;
    /** 已放弃或已取消。 */
    private long dropped;
    /** 已超时未响应邀约。 */
    private long timedOut;
    /** 全部候选人数量。 */
    private long all;
    /** 按筛选标签聚合的数量。 */
    private Map<JobCandidateListStatus, Long> statusCounts;

    public long getWaitingInvite() {
        return waitingInvite;
    }

    public void setWaitingInvite(long waitingInvite) {
        this.waitingInvite = waitingInvite;
    }

    public long getInvited() {
        return invited;
    }

    public void setInvited(long invited) {
        this.invited = invited;
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

    public long getTimedOut() {
        return timedOut;
    }

    public void setTimedOut(long timedOut) {
        this.timedOut = timedOut;
    }

    public long getAll() {
        return all;
    }

    public void setAll(long all) {
        this.all = all;
    }

    public Map<JobCandidateListStatus, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<JobCandidateListStatus, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }
}
