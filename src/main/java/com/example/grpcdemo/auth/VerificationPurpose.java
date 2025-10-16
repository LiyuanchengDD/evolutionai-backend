package com.example.grpcdemo.auth;

/**
 * Verification purposes for authentication related flows such as registration
 * and password reset.
 */
public enum VerificationPurpose {
    /** Email verification for new account registrations. */
    REGISTRATION,
    /** Verification when resetting an account password. */
    PASSWORD_RESET
}
