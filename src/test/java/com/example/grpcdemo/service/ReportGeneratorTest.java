package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.model.Candidate;
import com.example.grpcdemo.model.Interview;
import com.example.grpcdemo.model.Job;
import com.example.grpcdemo.repository.CandidateRepository;
import com.example.grpcdemo.repository.InterviewRepository;
import com.example.grpcdemo.repository.JobRepository;
import com.example.grpcdemo.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportGeneratorTest {

    private ReportRepository reportRepository;
    private InterviewRepository interviewRepository;
    private CandidateRepository candidateRepository;
    private JobRepository jobRepository;
    private AiEvaluationClient aiEvaluationClient;
    private ReportGenerator generator;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        interviewRepository = mock(InterviewRepository.class);
        candidateRepository = mock(CandidateRepository.class);
        jobRepository = mock(JobRepository.class);
        aiEvaluationClient = mock(AiEvaluationClient.class);
        generator = new ReportGenerator(reportRepository, interviewRepository, candidateRepository, jobRepository, aiEvaluationClient);
    }

    @Test
    void generateAndStore_buildsContentFromContext() {
        Interview interview = new Interview();
        interview.setId("int1");
        interview.setCandidateId("cand1");
        interview.setJobId("job1");
        interview.setScheduledTime("2024-04-20T12:00:00Z");
        interview.setStatus("COMPLETED");
        when(interviewRepository.findById("int1")).thenReturn(Optional.of(interview));

        Candidate candidate = new Candidate();
        candidate.setId("cand1");
        candidate.setName("Jane Doe");
        candidate.setEmail("jane@example.com");
        when(candidateRepository.findById("cand1")).thenReturn(Optional.of(candidate));

        Job job = new Job();
        job.setId("job1");
        job.setJobTitle("Backend Developer");
        when(jobRepository.findById("job1")).thenReturn(Optional.of(job));

        when(aiEvaluationClient.evaluate("int1"))
                .thenReturn(new EvaluationResult("Strong communication skills", 0.82f, "Great potential"));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReportEntity entity = generator.generateAndStore("int1");

        assertNotNull(entity.getReportId());
        assertEquals("int1", entity.getInterviewId());
        assertEquals(0.82f, entity.getScore());
        assertEquals("Great potential", entity.getEvaluatorComment());
        assertTrue(entity.getContent().contains("Jane Doe"));
        assertTrue(entity.getContent().contains("Backend Developer"));
        assertTrue(entity.getContent().contains("Strong communication skills"));
        assertTrue(entity.getContent().contains("Score: 0.82"));
        assertTrue(entity.getCreatedAt() > 0);
    }

    @Test
    void generateAndStore_missingInterviewThrowsException() {
        when(interviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(InterviewNotFoundException.class, () -> generator.generateAndStore("missing"));
        verify(reportRepository, never()).save(any());
    }

    @Test
    void generateAndStore_handlesMissingCandidateAndJob() {
        Interview interview = new Interview();
        interview.setId("int2");
        interview.setScheduledTime("2024-04-25T08:30:00Z");
        interview.setStatus("COMPLETED");
        when(interviewRepository.findById("int2")).thenReturn(Optional.of(interview));

        when(aiEvaluationClient.evaluate("int2"))
                .thenReturn(new EvaluationResult("Average performance", 0.5f, "Needs improvement"));
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReportEntity entity = generator.generateAndStore("int2");

        assertTrue(entity.getContent().contains("Unknown candidate"));
        assertTrue(entity.getContent().contains("Unknown role"));
        assertTrue(entity.getContent().contains("Average performance"));
    }
}
