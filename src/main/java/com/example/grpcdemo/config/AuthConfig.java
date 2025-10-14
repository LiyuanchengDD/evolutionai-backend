package com.example.grpcdemo.config;

import com.example.grpcdemo.auth.AuthManager;
import com.example.grpcdemo.auth.VerificationCodeSender;
import com.example.grpcdemo.repository.AuthVerificationCodeRepository;
import com.example.grpcdemo.repository.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * Authentication related service wiring.
 */
@Configuration
public class AuthConfig {

    @Bean
    public AuthManager authManager(UserAccountRepository userRepository,
                                   AuthVerificationCodeRepository verificationCodeRepository,
                                   PasswordEncoder passwordEncoder,
                                   VerificationCodeSender verificationCodeSender,
                                   SecureRandom secureRandom,
                                   Clock clock) {
        return new AuthManager(
                userRepository,
                verificationCodeRepository,
                passwordEncoder,
                verificationCodeSender,
                secureRandom,
                clock
        );
    }
}

