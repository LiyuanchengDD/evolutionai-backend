package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.GenerateReportRequest;
import com.example.grpcdemo.proto.GetReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import com.example.grpcdemo.proto.ReportServiceGrpc;
import io.grpc.Status;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class ReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private final Map<String, ReportResponse> reportStore = new ConcurrentHashMap<>();

    @Override
    public void generateReport(GenerateReportRequest request,
                               StreamObserver<ReportResponse> responseObserver) {
        String reportId = UUID.randomUUID().toString();
        ReportResponse response = ReportResponse.newBuilder()
                .setReportId(reportId)
                .setInterviewId(request.getInterviewId())
                .setContent("Report placeholder")
                .setScore(0)
                .setEvaluatorComment("")
                .setCreatedAt(System.currentTimeMillis())
                .build();
        reportStore.put(reportId, response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getReport(GetReportRequest request,
                          StreamObserver<ReportResponse> responseObserver) {
        ReportResponse response = reportStore.get(request.getReportId());
        if (response == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Report not found").asRuntimeException());
            return;
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
