package com.example.grpcdemo.events;

import com.example.grpcdemo.config.RabbitMQConfig;
import com.example.grpcdemo.model.Candidate;
import com.example.grpcdemo.repository.CandidateRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Sends notification emails once a report has been generated.
 */
@Component
@Profile("!test")
public class ReportGeneratedConsumer {

    private final CandidateRepository candidateRepository;
    private final JavaMailSender mailSender;

    public ReportGeneratedConsumer(CandidateRepository candidateRepository,
                                   JavaMailSender mailSender) {
        this.candidateRepository = candidateRepository;
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = RabbitMQConfig.REPORT_GENERATED_QUEUE)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void handleReportGenerated(ReportGeneratedEvent event) {
        Candidate candidate = candidateRepository.findById(event.candidateId())
                .orElseThrow(() -> new IllegalStateException("Candidate not found"));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(candidate.getEmail());
        message.setSubject("Interview report ready");
        message.setText("Your interview report is ready. Report ID: " + event.reportId());
        mailSender.send(message);
    }

    @Recover
    public void recover(Exception e, ReportGeneratedEvent event) {
        System.err.println("Failed to send notification for report " + event.reportId());
    }
}
