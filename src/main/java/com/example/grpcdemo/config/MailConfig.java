package com.example.grpcdemo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 提供一个兜底的 JavaMailSender（不配置主机时不会真正发信），
 * 仅用于满足对 JavaMailSender 的依赖注入，避免应用启动失败。
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        // 未设置 host/用户名/密码时，此 Bean 仅作为占位，不会实际发信
        return new JavaMailSenderImpl();
    }
}
