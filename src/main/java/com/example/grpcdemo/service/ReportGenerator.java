package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.model.Candidate;
import com.example.grpcdemo.model.Interview;
import com.example.grpcdemo.model.Job;
import com.example.grpcdemo.repository.CandidateRepository;
import com.example.grpcdemo.repository.InterviewRepository;
import com.example.grpcdemo.repository.JobRepository;
import com.example.grpcdemo.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates gathering interview context, invoking the AI evaluation and
 * storing the resulting report content.
 */
@Service
public class ReportGenerator {

    private final ReportRepository reportRepository;
    private final InterviewRepository interviewRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final AiEvaluationClient aiEvaluationClient;

    public ReportGenerator(ReportRepository reportRepository,
                           InterviewRepository interviewRepository,
                           CandidateRepository candidateRepository,
                           JobRepository jobRepository,
                           AiEvaluationClient aiEvaluationClient) {
        this.reportRepository = reportRepository;
        this.interviewRepository = interviewRepository;
        this.candidateRepository = candidateRepository;
        this.jobRepository = jobRepository;
        this.aiEvaluationClient = aiEvaluationClient;
    }

    /**
     * Generate a report for the supplied interview and persist it.
     *
     * @param interviewId the interview identifier
     * @return the persisted report entity
     */
    public ReportEntity generateAndStore(String interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));

        Candidate candidate = Optional.ofNullable(interview.getCandidateId())
                .flatMap(candidateRepository::findById)
                .orElse(null);
        Job job = Optional.ofNullable(interview.getJobId())
                .flatMap(jobRepository::findById)
                .orElse(null);

        EvaluationResult evaluation = aiEvaluationClient.evaluate(interviewId);
        String comment = evaluation.comment() != null ? evaluation.comment() : "";
        ReportEntity entity = new ReportEntity(
                UUID.randomUUID().toString(),
                interviewId,
                buildContent(interview, candidate, job, evaluation),
                evaluation.score(),
                comment,
                System.currentTimeMillis());
        return reportRepository.save(entity);
    }

    private String buildContent(Interview interview, Candidate candidate, Job job, EvaluationResult evaluation) {
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append("Interview Report").append(lineSeparator);
        builder.append("Candidate: ")
                .append(candidate != null && candidate.getName() != null ? candidate.getName() : "Unknown candidate")
                .append(lineSeparator);
        if (candidate != null && candidate.getEmail() != null && !candidate.getEmail().isBlank()) {
            builder.append("Email: ").append(candidate.getEmail()).append(lineSeparator);
        }
        builder.append("Job: ")
                .append(job != null && job.getJobTitle() != null ? job.getJobTitle() : "Unknown role")
                .append(lineSeparator);
        builder.append("Scheduled: ")
                .append(interview.getScheduledTime() != null && !interview.getScheduledTime().isBlank()
                        ? interview.getScheduledTime() : "unscheduled time")
                .append(lineSeparator);
        builder.append("Status: ")
                .append(interview.getStatus() != null ? interview.getStatus() : "UNKNOWN")
                .append(lineSeparator).append(lineSeparator);
        builder.append("AI Summary:").append(lineSeparator);
        builder.append(evaluation.content()).append(lineSeparator);
        if (evaluation.comment() != null && !evaluation.comment().isBlank()) {
            builder.append(lineSeparator)
                    .append("Evaluator Comment: ")
                    .append(evaluation.comment())
                    .append(lineSeparator);
        }
        builder.append("Score: ")
                .append(String.format(Locale.US, "%.2f", evaluation.score()));
        return builder.toString();
    }
}
