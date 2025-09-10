package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.ConfirmInterviewRequest;
import com.example.grpcdemo.proto.GetInterviewsByCandidateRequest;
import com.example.grpcdemo.proto.GetInterviewsByJobRequest;
import com.example.grpcdemo.proto.InterviewResponse;
import com.example.grpcdemo.proto.InterviewServiceGrpc;
import com.example.grpcdemo.proto.InterviewsResponse;
import com.example.grpcdemo.proto.ScheduleInterviewRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class InterviewServiceImpl extends InterviewServiceGrpc.InterviewServiceImplBase {

    private final Map<String, InterviewResponse> interviews = new ConcurrentHashMap<>();

    @Override
    public void scheduleInterview(ScheduleInterviewRequest request, StreamObserver<InterviewResponse> responseObserver) {
        String id = UUID.randomUUID().toString();
        InterviewResponse response = InterviewResponse.newBuilder()
                .setInterviewId(id)
                .setCandidateId(request.getCandidateId())
                .setJobId(request.getJobId())
                .setScheduledTime(request.getScheduledTime())
                .setStatus("SCHEDULED")
                .build();
        interviews.put(id, response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void confirmInterview(ConfirmInterviewRequest request, StreamObserver<InterviewResponse> responseObserver) {
        InterviewResponse existing = interviews.get(request.getInterviewId());
        if (existing == null) {
            responseObserver.onNext(InterviewResponse.newBuilder()
                    .setInterviewId(request.getInterviewId())
                    .setStatus("NOT_FOUND")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        String status = request.getAccepted() ? "CONFIRMED" : "REJECTED";
        InterviewResponse updated = existing.toBuilder().setStatus(status).build();
        interviews.put(existing.getInterviewId(), updated);
        responseObserver.onNext(updated);
        responseObserver.onCompleted();
    }

    @Override
    public void getInterviewsByCandidate(GetInterviewsByCandidateRequest request, StreamObserver<InterviewsResponse> responseObserver) {
        List<InterviewResponse> list = new ArrayList<>();
        for (InterviewResponse i : interviews.values()) {
            if (i.getCandidateId().equals(request.getCandidateId())) {
                list.add(i);
            }
        }
        responseObserver.onNext(InterviewsResponse.newBuilder().addAllInterviews(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getInterviewsByJob(GetInterviewsByJobRequest request, StreamObserver<InterviewsResponse> responseObserver) {
        List<InterviewResponse> list = new ArrayList<>();
        for (InterviewResponse i : interviews.values()) {
            if (i.getJobId().equals(request.getJobId())) {
                list.add(i);
            }
        }
        responseObserver.onNext(InterviewsResponse.newBuilder().addAllInterviews(list).build());
        responseObserver.onCompleted();
    }
}

