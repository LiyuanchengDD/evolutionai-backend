package com.example.grpcdemo.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Sends verification codes via email using {@link JavaMailSender}.
 */
@Component
public class MailVerificationCodeSender implements VerificationCodeSender {

    private static final Logger log = LoggerFactory.getLogger(MailVerificationCodeSender.class);

    private final JavaMailSender mailSender;

    public MailVerificationCodeSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String email,
                                      String code,
                                      AuthRole role,
                                      VerificationPurpose purpose,
                                      Duration ttl) {
        String subject = buildSubject(purpose);
        String body = buildBody(code, ttl);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Sent {} verification code to email={}, role={}", purpose, email, role);
        } catch (MailException ex) {
            log.error("Failed to send verification code email to {}", email, ex);
            throw new AuthException(AuthErrorCode.INTERNAL_ERROR, "验证码发送失败，请稍后再试");
        }
    }

    private String buildSubject(VerificationPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> "【EvolutionAI】注册验证码";
            case RESET_PASSWORD -> "【EvolutionAI】密码重置验证码";
        };
    }

    private String buildBody(String code, Duration ttl) {
        long minutes = Math.max(1, ttl.toMinutes());
        return String.format("您的验证码为 %s ，有效期 %d 分钟。如非本人操作，请忽略本邮件。", code, minutes);
    }
}
