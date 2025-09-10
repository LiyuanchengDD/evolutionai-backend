package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.*;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class InterviewServiceImpl extends InterviewServiceGrpc.InterviewServiceImplBase {

    private final List<InterviewResponse> interviews = new ArrayList<>();

    @Override
    public void scheduleInterview(ScheduleInterviewRequest request,
                                  StreamObserver<InterviewResponse> responseObserver) {
        String interviewId = UUID.randomUUID().toString();

        InterviewResponse interview = InterviewResponse.newBuilder()
                .setInterviewId(interviewId)
                .setCandidateId(request.getCandidateId())
                .setJobId(request.getJobId())
                .setScheduledTime(request.getScheduledTime())
                .setStatus("Scheduled")
                .build();

        interviews.add(interview);

        responseObserver.onNext(interview);
        responseObserver.onCompleted();
    }

    @Override
    public void confirmInterview(ConfirmInterviewRequest request,
                                 StreamObserver<InterviewResponse> responseObserver) {
        for (int i = 0; i < interviews.size(); i++) {
            InterviewResponse interview = interviews.get(i);
            if (interview.getInterviewId().equals(request.getInterviewId())) {
                String status = request.getAccepted() ? "Confirmed" : "Rejected";

                InterviewResponse updated = interview.toBuilder()
                        .setStatus(status)
                        .build();

                interviews.set(i, updated);

                responseObserver.onNext(updated);
                responseObserver.onCompleted();
                return;
            }
        }
        responseObserver.onError(new IllegalArgumentException("Interview not found"));
    }

    @Override
    public void getInterviewsByCandidate(GetInterviewsByCandidateRequest request,
                                         StreamObserver<InterviewsResponse> responseObserver) {
        List<InterviewResponse> result = interviews.stream()
                .filter(interview -> interview.getCandidateId().equals(request.getCandidateId()))
                .collect(Collectors.toList());

        InterviewsResponse response = InterviewsResponse.newBuilder()
                .addAllInterviews(result)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getInterviewsByJob(GetInterviewsByJobRequest request,
                                   StreamObserver<InterviewsResponse> responseObserver) {
        List<InterviewResponse> result = interviews.stream()
                .filter(interview -> interview.getJobId().equals(request.getJobId()))
                .collect(Collectors.toList());

        InterviewsResponse response = InterviewsResponse.newBuilder()
                .addAllInterviews(result)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
