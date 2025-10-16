package com.example.grpcdemo.security.trial;

public enum TrialStatusCode {
    VALID("valid"),
    NOT_SENT("not_sent"),
    EXPIRED("expired"),
    REDEEMED("redeemed");

    private final String responseValue;

    TrialStatusCode(String responseValue) {
        this.responseValue = responseValue;
    }

    public String getResponseValue() {
        return responseValue;
    }
}

