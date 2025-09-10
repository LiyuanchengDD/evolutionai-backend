package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CreateReportRequest;
import com.example.grpcdemo.proto.ReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import com.example.grpcdemo.proto.ReportServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class ReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private final Map<String, ReportResponse> reports = new ConcurrentHashMap<>();

    @Override
    public void createReport(CreateReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        String id = UUID.randomUUID().toString();
        String now = Instant.now().toString();
        ReportResponse response = ReportResponse.newBuilder()
                .setReportId(id)
                .setInterviewId(request.getInterviewId())
                .setScore(request.getScore())
                .setEvaluatorComment(request.getEvaluatorComment())
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .build();
        reports.put(id, response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getReport(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        ReportResponse found = reports.getOrDefault(request.getReportId(),
                ReportResponse.newBuilder()
                        .setReportId(request.getReportId())
                        .setEvaluatorComment("NOT_FOUND")
                        .build());
        responseObserver.onNext(found);
        responseObserver.onCompleted();
    }
}

