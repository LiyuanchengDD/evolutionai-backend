package com.example.grpcdemo.security.trial;

import java.time.Instant;

public record TrialStatus(TrialStatusCode status, Instant sentAt, Instant expiresAt, Instant redeemedAt) {

    public static final String REQUEST_ATTRIBUTE = "app.trial.status";

    public boolean isAccessGranted() {
        return status == TrialStatusCode.VALID || status == TrialStatusCode.REDEEMED;
    }
}

