package com.example.grpcdemo.security.trial;

final class TrialExceptions {

    private TrialExceptions() {
    }

    static TrialException inviteNotSent() {
        return new TrialException(TrialErrorCode.TRIAL_INVITE_NOT_SENT);
    }

    static TrialException inviteExpired() {
        return new TrialException(TrialErrorCode.TRIAL_INVITE_EXPIRED);
    }

    static TrialException invalidCode() {
        return new TrialException(TrialErrorCode.TRIAL_INVITE_INVALID_CODE);
    }

    static TrialException nonEnterpriseUser() {
        return new TrialException(TrialErrorCode.NOT_ENTERPRISE_USER);
    }

    static TrialException invalidEnterpriseProfile() {
        return new TrialException(TrialErrorCode.ENTERPRISE_PROFILE_INCOMPLETE);
    }
}
