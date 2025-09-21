package com.example.grpcdemo.events;

import com.example.grpcdemo.config.RabbitMQConfig;
import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.repository.ReportRepository;
import com.example.grpcdemo.service.AiEvaluationClient;
import com.example.grpcdemo.service.EvaluationResult;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes interview completed events and generates reports.
 */
@Component
@Profile("!test")
public class InterviewCompletedConsumer {

    private final AiEvaluationClient aiEvaluationClient;
    private final ReportRepository reportRepository;
    private final RabbitTemplate rabbitTemplate;

    public InterviewCompletedConsumer(AiEvaluationClient aiEvaluationClient,
                                      ReportRepository reportRepository,
                                      RabbitTemplate rabbitTemplate) {
        this.aiEvaluationClient = aiEvaluationClient;
        this.reportRepository = reportRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.INTERVIEW_COMPLETED_QUEUE)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void handleInterviewCompleted(InterviewCompletedEvent event) {
        EvaluationResult result = aiEvaluationClient.evaluate(event.interviewId());
        ReportEntity entity = new ReportEntity(
                UUID.randomUUID().toString(),
                event.interviewId(),
                result.content(),
                result.score(),
                result.comment(),
                System.currentTimeMillis());
        reportRepository.save(entity);
        rabbitTemplate.convertAndSend(RabbitMQConfig.REPORT_GENERATED_QUEUE,
                new ReportGeneratedEvent(entity.getReportId(), event.interviewId(), event.candidateId()));
    }

    @Recover
    public void recover(Exception e, InterviewCompletedEvent event) {
        System.err.println("Failed to generate report for interview " + event.interviewId());
    }
}
