package com.example.grpcdemo.mail;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A {@link JavaMailSender} implementation that only logs messages instead of sending them.
 * <p>
 * Serves as a safe fallback when no SMTP configuration is provided so the application can
 * still start and execute email-related workflows without failing at runtime.
 */
public class NoopMailSender implements JavaMailSender {

    private static final Logger log = LoggerFactory.getLogger(NoopMailSender.class);

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage((Session) null);
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) {
        try {
            return new MimeMessage(null, contentStream);
        } catch (MessagingException e) {
            throw new MailPreparationException("Failed to create MimeMessage from stream", e);
        }
    }

    @Override
    public void send(MimeMessage mimeMessage) {
        logMimeMessage("send", mimeMessage);
    }

    @Override
    public void send(MimeMessage... mimeMessages) {
        if (mimeMessages != null) {
            Arrays.stream(mimeMessages).forEach(message -> logMimeMessage("send[]", message));
        }
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) {
        MimeMessage message = createMimeMessage();
        prepareMessage(mimeMessagePreparator, message);
        send(message);
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) {
        if (mimeMessagePreparators != null) {
            Arrays.stream(mimeMessagePreparators).forEach(preparator -> {
                MimeMessage message = createMimeMessage();
                prepareMessage(preparator, message);
                send(message);
            });
        }
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) {
        logSimpleMessage("send", simpleMessage);
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) {
        if (simpleMessages != null) {
            Arrays.stream(simpleMessages).forEach(message -> logSimpleMessage("send[]", message));
        }
    }

    private void prepareMessage(MimeMessagePreparator preparator, MimeMessage message) {
        try {
            preparator.prepare(message);
        } catch (Exception e) {
            throw new MailPreparationException("Failed to prepare MimeMessage", e);
        }
    }

    private void logSimpleMessage(String action, @Nullable SimpleMailMessage message) {
        if (message == null) {
            return;
        }
        log.info("Noop mail sender - {} simple message: to={}, cc={}, subject={}",
                action,
                safeJoin(message.getTo()),
                safeJoin(message.getCc()),
                message.getSubject());
    }

    private void logMimeMessage(String action, @Nullable MimeMessage message) {
        if (message == null) {
            return;
        }
        try {
            Address[] recipients = message.getAllRecipients();
            log.info("Noop mail sender - {} mime message: to={}, subject={}",
                    action,
                    recipients == null ? "" : Arrays.stream(recipients)
                            .filter(Objects::nonNull)
                            .map(Address::toString)
                            .collect(Collectors.joining(",")),
                    message.getSubject());
        } catch (MessagingException e) {
            log.info("Noop mail sender - {} mime message (failed to read headers): {}", action, e.getMessage());
        }
    }

    private String safeJoin(@Nullable String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }
}
