package com.example.grpcdemo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * 提供 Auth 模块所需的通用 Bean，避免因为缺少依赖而导致应用启动失败。
 */
@Configuration
public class AuthUtilityConfig {

    @Bean
    @ConditionalOnMissingBean(SecureRandom.class)
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock() {
        return Clock.systemUTC();
    }
}

