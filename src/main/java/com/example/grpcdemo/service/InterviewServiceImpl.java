package com.example.grpcdemo.service;

import com.example.grpcdemo.model.Interview;
import com.example.grpcdemo.proto.ConfirmInterviewRequest;
import com.example.grpcdemo.proto.GetInterviewsByCandidateRequest;
import com.example.grpcdemo.proto.GetInterviewsByJobRequest;
import com.example.grpcdemo.proto.InterviewRequest;
import com.example.grpcdemo.proto.InterviewResponse;
import com.example.grpcdemo.proto.InterviewServiceGrpc;
import com.example.grpcdemo.proto.InterviewsResponse;
import com.example.grpcdemo.proto.ScheduleInterviewRequest;
import com.example.grpcdemo.repository.InterviewRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC service for managing interviews backed by JPA.
 */
@GrpcService
public class InterviewServiceImpl extends InterviewServiceGrpc.InterviewServiceImplBase {

    private final InterviewRepository repository;

    public InterviewServiceImpl(InterviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public void scheduleInterview(ScheduleInterviewRequest request, StreamObserver<InterviewResponse> responseObserver) {
        Interview interview = new Interview();
        interview.setId(UUID.randomUUID().toString());
        interview.setCandidateId(request.getCandidateId());
        interview.setJobId(request.getJobId());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setStatus("SCHEDULED");
        Interview saved = repository.save(interview);
        responseObserver.onNext(toResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    public void confirmInterview(ConfirmInterviewRequest request, StreamObserver<InterviewResponse> responseObserver) {
        Interview interview = repository.findById(request.getInterviewId()).orElse(null);
        if (interview == null) {
            responseObserver.onNext(InterviewResponse.newBuilder()
                    .setInterviewId(request.getInterviewId())
                    .setStatus("NOT_FOUND")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        interview.setStatus(request.getAccepted() ? "CONFIRMED" : "REJECTED");
        Interview updated = repository.save(interview);
        responseObserver.onNext(toResponse(updated));
        responseObserver.onCompleted();
    }

    @Override
    public void getInterview(InterviewRequest request, StreamObserver<InterviewResponse> responseObserver) {
        InterviewResponse response = repository.findById(request.getInterviewId())
                .map(this::toResponse)
                .orElse(InterviewResponse.newBuilder()
                        .setInterviewId(request.getInterviewId())
                        .setStatus("NOT_FOUND")
                        .build());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void completeInterview(InterviewRequest request, StreamObserver<InterviewResponse> responseObserver) {
        Interview interview = repository.findById(request.getInterviewId()).orElse(null);
        if (interview == null) {
            responseObserver.onNext(InterviewResponse.newBuilder()
                    .setInterviewId(request.getInterviewId())
                    .setStatus("NOT_FOUND")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        interview.setStatus("COMPLETED");
        Interview updated = repository.save(interview);
        responseObserver.onNext(toResponse(updated));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteInterview(InterviewRequest request, StreamObserver<InterviewResponse> responseObserver) {
        InterviewResponse response = repository.findById(request.getInterviewId())
                .map(interview -> {
                    repository.delete(interview);
                    return InterviewResponse.newBuilder()
                            .setInterviewId(request.getInterviewId())
                            .setStatus("DELETED")
                            .build();
                })
                .orElse(InterviewResponse.newBuilder()
                        .setInterviewId(request.getInterviewId())
                        .setStatus("NOT_FOUND")
                        .build());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getInterviewsByCandidate(GetInterviewsByCandidateRequest request, StreamObserver<InterviewsResponse> responseObserver) {
        List<InterviewResponse> list = repository.findByCandidateId(request.getCandidateId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        responseObserver.onNext(InterviewsResponse.newBuilder().addAllInterviews(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getInterviewsByJob(GetInterviewsByJobRequest request, StreamObserver<InterviewsResponse> responseObserver) {
        List<InterviewResponse> list = repository.findByJobId(request.getJobId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        responseObserver.onNext(InterviewsResponse.newBuilder().addAllInterviews(list).build());
        responseObserver.onCompleted();
    }

    private InterviewResponse toResponse(Interview interview) {
        return InterviewResponse.newBuilder()
                .setInterviewId(interview.getId())
                .setCandidateId(interview.getCandidateId())
                .setJobId(interview.getJobId())
                .setScheduledTime(interview.getScheduledTime())
                .setStatus(interview.getStatus())
                .build();
    }
}

