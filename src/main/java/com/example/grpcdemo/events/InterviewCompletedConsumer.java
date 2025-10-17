package com.example.grpcdemo.events;

import com.example.grpcdemo.config.RabbitMQConfig;
import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.service.ReportGenerator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Consumes interview completed events and generates reports.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "app.rabbit", name = "enabled", havingValue = "true")
public class InterviewCompletedConsumer {

    private final ReportGenerator reportGenerator;
    private final RabbitTemplate rabbitTemplate;

    public InterviewCompletedConsumer(ReportGenerator reportGenerator,
                                      RabbitTemplate rabbitTemplate) {
        this.reportGenerator = reportGenerator;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.INTERVIEW_COMPLETED_QUEUE)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void handleInterviewCompleted(InterviewCompletedEvent event) {
        ReportEntity entity = reportGenerator.generateAndStore(event.interviewId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.REPORT_GENERATED_QUEUE,
                new ReportGeneratedEvent(entity.getReportId(), event.interviewId(), event.candidateId()));
    }

    @Recover
    public void recover(Exception e, InterviewCompletedEvent event) {
        System.err.println("Failed to generate report for interview " + event.interviewId());
    }
}
