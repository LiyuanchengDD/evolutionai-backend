package com.example.grpcdemo.onboarding;

/**
 * Exception thrown when enterprise onboarding workflow encounters recoverable errors.
 */
public class OnboardingException extends RuntimeException {

    private final OnboardingErrorCode errorCode;

    public OnboardingException(OnboardingErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public OnboardingException(OnboardingErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OnboardingErrorCode getErrorCode() {
        return errorCode;
    }
}
