package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CandidateResponse;
import com.example.grpcdemo.proto.ConfirmInterviewRequest;
import com.example.grpcdemo.proto.CreateCandidateRequest;
import com.example.grpcdemo.proto.CreateJobRequest;
import com.example.grpcdemo.proto.GetInterviewsByCandidateRequest;
import com.example.grpcdemo.proto.InterviewResponse;
import com.example.grpcdemo.proto.InterviewsResponse;
import com.example.grpcdemo.proto.JobResponse;
import com.example.grpcdemo.proto.ScheduleInterviewRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests verifying basic CRUD and state transitions for gRPC services.
 */
@SpringBootTest
class ServiceIntegrationTest {

    @Autowired
    private CandidateServiceImpl candidateService;
    @Autowired
    private JobServiceImpl jobService;
    @Autowired
    private InterviewServiceImpl interviewService;

    @Test
    void fullWorkflow() {
        // create candidate
        ResponseObserver<CandidateResponse> candObs = new ResponseObserver<>();
        candidateService.createCandidate(CreateCandidateRequest.newBuilder()
                .setName("Alice")
                .setEmail("alice@example.com")
                .setPhone("123")
                .build(), candObs);
        CandidateResponse candidate = candObs.value;
        Assertions.assertEquals("CREATED", candidate.getStatus());

        // create job
        ResponseObserver<JobResponse> jobObs = new ResponseObserver<>();
        jobService.createJob(CreateJobRequest.newBuilder()
                .setJobTitle("Dev")
                .setDescription("Desc")
                .build(), jobObs);
        JobResponse job = jobObs.value;
        Assertions.assertEquals("CREATED", job.getStatus());

        // schedule interview
        ResponseObserver<InterviewResponse> interviewObs = new ResponseObserver<>();
        interviewService.scheduleInterview(ScheduleInterviewRequest.newBuilder()
                .setCandidateId(candidate.getCandidateId())
                .setJobId(job.getJobId())
                .setScheduledTime("2025-09-11T10:00:00Z")
                .build(), interviewObs);
        InterviewResponse interview = interviewObs.value;
        Assertions.assertEquals("SCHEDULED", interview.getStatus());

        // confirm interview
        ResponseObserver<InterviewResponse> confirmObs = new ResponseObserver<>();
        interviewService.confirmInterview(ConfirmInterviewRequest.newBuilder()
                .setInterviewId(interview.getInterviewId())
                .setAccepted(true)
                .build(), confirmObs);
        InterviewResponse confirmed = confirmObs.value;
        Assertions.assertEquals("CONFIRMED", confirmed.getStatus());

        // list interviews by candidate
        ResponseObserver<InterviewsResponse> listObs = new ResponseObserver<>();
        interviewService.getInterviewsByCandidate(GetInterviewsByCandidateRequest.newBuilder()
                .setCandidateId(candidate.getCandidateId())
                .build(), listObs);
        InterviewsResponse interviews = listObs.value;
        Assertions.assertEquals(1, interviews.getInterviewsCount());
    }

    /** Simple StreamObserver capturing the last value. */
    private static class ResponseObserver<T> implements StreamObserver<T> {
        private T value;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            throw new RuntimeException(t);
        }

        @Override
        public void onCompleted() {
            // no-op
        }
    }
}

