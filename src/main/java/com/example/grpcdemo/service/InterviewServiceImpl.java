package com.example.grpcdemo.service;

import com.example.grpcdemo.config.RabbitMQConfig;
import com.example.grpcdemo.events.InterviewCompletedEvent;
import com.example.grpcdemo.model.Interview;
import com.example.grpcdemo.proto.ConfirmInterviewRequest;
import com.example.grpcdemo.proto.GetInterviewsByCandidateRequest;
import com.example.grpcdemo.proto.GetInterviewsByJobRequest;
import com.example.grpcdemo.proto.InterviewResponse;
import com.example.grpcdemo.proto.InterviewServiceGrpc;
import com.example.grpcdemo.proto.InterviewsResponse;
import com.example.grpcdemo.proto.ScheduleInterviewRequest;
import com.example.grpcdemo.repository.InterviewRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * gRPC service for managing interviews backed by JPA.
 */
@GrpcService
public class InterviewServiceImpl extends InterviewServiceGrpc.InterviewServiceImplBase {

    private final InterviewRepository repository;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    public InterviewServiceImpl(InterviewRepository repository,
                                ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        this.repository = repository;
        this.rabbitTemplateProvider = rabbitTemplateProvider;
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
        if (request.getAccepted()) {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend(RabbitMQConfig.INTERVIEW_COMPLETED_QUEUE,
                        new InterviewCompletedEvent(updated.getId(), updated.getCandidateId()));
            }
        }
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

