package com.example.grpcdemo.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Provides a fallback {@link JavaMailSender} implementation for local development.
 *
 * <p>The service currently only logs outgoing mail requests instead of sending them.
 * This allows the application context to start even when no SMTP server is configured.</p>
 */
@Configuration
public class StubMailConfiguration {

    @Bean
    public JavaMailSender stubJavaMailSender() {
        return new LoggingJavaMailSender();
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
                throw new MailPreparationException("Failed to create MimeMessage from stream", ex);
            }
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            log.info("Pretending to send MIME message: subject='{}'", mimeMessage.getSubject());
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
                throw new MailPreparationException("Failed to prepare MIME message", ex);
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
            log.info("Pretending to send simple mail: to='{}', subject='{}'", Arrays.toString(simpleMessage.getTo()), simpleMessage.getSubject());
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            Arrays.stream(simpleMessages).forEach(this::send);
        }
    }
}

