package com.example.grpcdemo.auth;

import java.time.Duration;

/**
 * Abstraction for delivering verification codes to users.
 */
public interface VerificationCodeSender {

    /**
     * Sends a verification code to the user.
     *
     * @param email   target email address
     * @param code    generated verification code
     * @param role    user role associated with the request
     * @param purpose purpose of the verification code
     * @param ttl     validity duration of the verification code
     */
    void sendVerificationCode(String email,
                              String code,
                              AuthRole role,
                              VerificationPurpose purpose,
                              Duration ttl);
}
