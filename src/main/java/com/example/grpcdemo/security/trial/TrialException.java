package com.example.grpcdemo.security.trial;

public class TrialException extends RuntimeException {

    private final TrialErrorCode errorCode;

    public TrialException(TrialErrorCode errorCode) {
        this(errorCode, null);
    }

    public TrialException(TrialErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public TrialErrorCode getErrorCode() {
        return errorCode;
    }
}
