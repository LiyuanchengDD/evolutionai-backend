package com.example.grpcdemo.security.trial;

import java.time.Instant;

public record TrialStatus(TrialStatusCode status, Instant expiresAt) {

    public static final String REQUEST_ATTRIBUTE = "app.trial.status";

    public boolean isActive() {
        return status == TrialStatusCode.ACTIVE;
    }
}

