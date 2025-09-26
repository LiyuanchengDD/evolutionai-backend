package com.example.grpcdemo.auth;

/**
 * Represents the purpose of a verification code request.
 */
public enum VerificationPurpose {
    /**
     * Used when a user is registering a brand-new account.
     */
    REGISTER,

    /**
     * Used when a user is resetting a forgotten password.
     */
    RESET_PASSWORD
}
