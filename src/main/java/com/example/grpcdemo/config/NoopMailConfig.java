package com.example.grpcdemo.config;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * 提供一个空操作（No-op）的 {@link JavaMailSender} 实现，
 * 用于在本地开发或未配置邮件服务器时让应用顺利启动。
 *
 * <p>该实现仅会记录即将发送的邮件内容，而不会真正发送邮件。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.mail", name = "stub-enabled", havingValue = "true", matchIfMissing = true)
public class NoopMailConfig {

    @Bean
    @Primary
    public JavaMailSender noopJavaMailSender() {
        logFallbackActivation();
        return new LoggingJavaMailSender();
    }

    private void logFallbackActivation() {
        Logger logger = LoggerFactory.getLogger(NoopMailConfig.class);
        if (logger.isWarnEnabled()) {
            logger.warn("No JavaMailSender bean found; falling back to logging-only mail sender. "
                    + "Set up spring.mail.* properties or provide a custom JavaMailSender bean "
                    + "to enable real email delivery.");
        }
    }

    private static class LoggingJavaMailSender implements JavaMailSender {

        private static final Logger log = LoggerFactory.getLogger(LoggingJavaMailSender.class);
        private final Session session = Session.getDefaultInstance(new Properties());

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(session);
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) {
            try {
                return new MimeMessage(session, contentStream);
            } catch (Exception ex) {
                throw new MailPreparationException("无法根据输入流创建 MimeMessage", ex);
            }
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            try {
                log.info("[NoopMailSender] Pretending to send MIME message: subject='{}'", mimeMessage.getSubject());
            } catch (MessagingException ex) {
                log.warn("[NoopMailSender] Pretending to send MIME message but failed to resolve subject", ex);
            }
        }

        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            Arrays.stream(mimeMessages).forEach(this::send);
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
            MimeMessage mimeMessage = createMimeMessage();
            try {
                mimeMessagePreparator.prepare(mimeMessage);
            } catch (Exception ex) {
                throw new MailPreparationException("无法准备 MimeMessage", ex);
            }
            send(mimeMessage);
        }

        @Override
        public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
            for (MimeMessagePreparator preparator : mimeMessagePreparators) {
                send(preparator);
            }
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            log.info("[NoopMailSender] Pretending to send simple mail: to='{}', subject='{}'", Arrays.toString(simpleMessage.getTo()), simpleMessage.getSubject());
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            Arrays.stream(simpleMessages).forEach(this::send);
        }
    }
}
