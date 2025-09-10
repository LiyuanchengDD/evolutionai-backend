package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.GenerateReportRequest;
import com.example.grpcdemo.proto.GetReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import com.example.grpcdemo.proto.ReportServiceGrpc;

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
                .build();
        reportStore.put(reportId, response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getReport(GetReportRequest request,
                          StreamObserver<ReportResponse> responseObserver) {
        ReportResponse response = reportStore.getOrDefault(
                request.getReportId(),
                ReportResponse.newBuilder()
                        .setReportId(request.getReportId())
                        .setInterviewId("")
                        .setContent("Not Found")
                        .build());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
