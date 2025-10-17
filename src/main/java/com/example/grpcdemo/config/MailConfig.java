package com.example.grpcdemo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import com.example.grpcdemo.mail.NoopMailSender;

/**
 * 提供一个兜底的 JavaMailSender（不配置主机时不会真正发信），
 * 仅用于满足对 JavaMailSender 的依赖注入，避免应用启动失败。
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        // 默认提供一个不会实际发信的 MailSender，保证依赖注入成功
        return new NoopMailSender();
    }
}
