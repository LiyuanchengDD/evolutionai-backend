package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.model.Candidate;
import com.example.grpcdemo.model.Interview;
import com.example.grpcdemo.model.Job;
import com.example.grpcdemo.proto.GenerateReportRequest;
import com.example.grpcdemo.proto.GetReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import com.example.grpcdemo.repository.CandidateRepository;
import com.example.grpcdemo.repository.InterviewRepository;
import com.example.grpcdemo.repository.JobRepository;
import com.example.grpcdemo.repository.ReportRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReportServiceIntegrationTest {

    @Autowired
    private ReportServiceImpl service;

    @Autowired
    private ReportRepository repository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ReportGenerator reportGenerator;

    @MockBean
    private AiEvaluationClient aiEvaluationClient;

    @AfterEach
    void cleanDatabase() {
        repository.deleteAll();
        interviewRepository.deleteAll();
        candidateRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void generateReport_persistsToDatabase() {
        Candidate candidate = new Candidate();
        candidate.setId("cand1");
        candidate.setName("Ada Lovelace");
        candidate.setEmail("ada@example.com");
        candidateRepository.save(candidate);

        Job job = new Job();
        job.setId("job1");
        job.setJobTitle("Platform Engineer");
        jobRepository.save(job);

        Interview interview = new Interview();
        interview.setId("int1");
        interview.setCandidateId("cand1");
        interview.setJobId("job1");
        interview.setScheduledTime("2024-05-01T09:00:00Z");
        interview.setStatus("COMPLETED");
        interviewRepository.save(interview);

        when(aiEvaluationClient.evaluate("int1"))
                .thenReturn(new EvaluationResult("content", 5.0f, "nice"));

        TestObserver observer = new TestObserver();
        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("int1").build(), observer);

        assertNotNull(observer.value);
        Optional<ReportEntity> entity = repository.findById(observer.value.getReportId());
        assertTrue(entity.isPresent());
        assertTrue(entity.get().getContent().contains("Ada Lovelace"));
        assertTrue(entity.get().getContent().contains("Platform Engineer"));
        assertTrue(entity.get().getContent().contains("content"));

        TestObserver getObserver = new TestObserver();
        service.getReport(GetReportRequest.newBuilder().setReportId(observer.value.getReportId()).build(), getObserver);
        assertTrue(getObserver.value.getContent().contains("content"));
        assertTrue(getObserver.value.getContent().contains("Ada Lovelace"));
    }

    @Test
    void reportIsRetrievableAfterServiceRecreation() {
        Candidate candidate = new Candidate();
        candidate.setId("cand2");
        candidate.setName("Grace Hopper");
        candidateRepository.save(candidate);

        Job job = new Job();
        job.setId("job2");
        job.setJobTitle("Backend Developer");
        jobRepository.save(job);

        Interview interview = new Interview();
        interview.setId("int2");
        interview.setCandidateId("cand2");
        interview.setJobId("job2");
        interview.setScheduledTime("2024-05-02T10:30:00Z");
        interview.setStatus("COMPLETED");
        interviewRepository.save(interview);

        when(aiEvaluationClient.evaluate("int2"))
                .thenReturn(new EvaluationResult("analysis", 4.2f, "solid candidate"));

        TestObserver observer = new TestObserver();
        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("int2").build(), observer);

        assertNotNull(observer.value);
        ReportServiceImpl newInstance = new ReportServiceImpl(repository, reportGenerator);
        TestObserver secondObserver = new TestObserver();
        newInstance.getReport(GetReportRequest.newBuilder()
                .setReportId(observer.value.getReportId())
                .build(), secondObserver);

        assertNull(secondObserver.error);
        assertNotNull(secondObserver.value);
        assertTrue(secondObserver.value.getContent().contains("analysis"));
        assertEquals(4.2f, secondObserver.value.getScore());
        assertEquals("solid candidate", secondObserver.value.getEvaluatorComment());
    }

    @Test
    void getReport_notFoundReturnsError() {
        TestObserver observer = new TestObserver();
        service.getReport(GetReportRequest.newBuilder().setReportId("missing").build(), observer);
        assertNotNull(observer.error);
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(observer.error).getCode());
    }

    private static class TestObserver implements StreamObserver<ReportResponse> {
        ReportResponse value;
        Throwable error;

        @Override
        public void onNext(ReportResponse value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
        }
    }
}

