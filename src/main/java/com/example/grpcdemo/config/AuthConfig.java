// src/main/java/com/example/grpcdemo/config/AuthConfig.java
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
import java.util.Optional;

@Configuration
public class AuthConfig {

    @Bean
    public AuthManager authManager(UserAccountRepository userRepository,
                                   AuthVerificationCodeRepository verificationCodeRepository,
                                   PasswordEncoder passwordEncoder,
                                   VerificationCodeSender verificationCodeSender,
                                   Optional<SecureRandom> secureRandom,
                                   Optional<Clock> clock) {
        return new AuthManager(
                userRepository,
                verificationCodeRepository,
                passwordEncoder,
                verificationCodeSender,
                secureRandom.orElseGet(SecureRandom::new),
                clock.orElseGet(Clock::systemUTC)
        );
    }

    // 如无现成 Bean，可顺手提供：
    @Bean SecureRandom secureRandom() { return new SecureRandom(); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
