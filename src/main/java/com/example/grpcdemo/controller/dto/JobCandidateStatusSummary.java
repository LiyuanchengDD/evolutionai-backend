package com.example.grpcdemo.controller.dto;

/**
 * 岗位候选人状态聚合。
 */
public class JobCandidateStatusSummary {

    private long invitePending;
    private long inviteSent;
    private long inviteFailed;
    private long emailMissing;
    private long interviewNotStarted;
    private long interviewScheduled;
    private long interviewInProgress;
    private long interviewCompleted;
    private long interviewCancelled;
    private long interviewAbandoned;
    private long interviewTimedOut;

    public long getInvitePending() {
        return invitePending;
    }

    public void setInvitePending(long invitePending) {
        this.invitePending = invitePending;
    }

    public long getInviteSent() {
        return inviteSent;
    }

    public void setInviteSent(long inviteSent) {
        this.inviteSent = inviteSent;
    }

    public long getInviteFailed() {
        return inviteFailed;
    }

    public void setInviteFailed(long inviteFailed) {
        this.inviteFailed = inviteFailed;
    }

    public long getEmailMissing() {
        return emailMissing;
    }

    public void setEmailMissing(long emailMissing) {
        this.emailMissing = emailMissing;
    }

    public long getInterviewNotStarted() {
        return interviewNotStarted;
    }

    public void setInterviewNotStarted(long interviewNotStarted) {
        this.interviewNotStarted = interviewNotStarted;
    }

    public long getInterviewScheduled() {
        return interviewScheduled;
    }

    public void setInterviewScheduled(long interviewScheduled) {
        this.interviewScheduled = interviewScheduled;
    }

    public long getInterviewInProgress() {
        return interviewInProgress;
    }

    public void setInterviewInProgress(long interviewInProgress) {
        this.interviewInProgress = interviewInProgress;
    }

    public long getInterviewCompleted() {
        return interviewCompleted;
    }

    public void setInterviewCompleted(long interviewCompleted) {
        this.interviewCompleted = interviewCompleted;
    }

    public long getInterviewCancelled() {
        return interviewCancelled;
    }

    public void setInterviewCancelled(long interviewCancelled) {
        this.interviewCancelled = interviewCancelled;
    }

    public long getInterviewAbandoned() {
        return interviewAbandoned;
    }

    public void setInterviewAbandoned(long interviewAbandoned) {
        this.interviewAbandoned = interviewAbandoned;
    }

    public long getInterviewTimedOut() {
        return interviewTimedOut;
    }

    public void setInterviewTimedOut(long interviewTimedOut) {
        this.interviewTimedOut = interviewTimedOut;
    }
}
