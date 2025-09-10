package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CreateReportRequest;
import com.example.grpcdemo.proto.ReportRequest;
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
    public void createReport(CreateReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        String id = UUID.randomUUID().toString();
        ReportResponse report = ReportResponse.newBuilder()
                .setReportId(id)
                .setContent(request.getContent())
                .build();
        reportStore.put(id, report);
        responseObserver.onNext(report);
        responseObserver.onCompleted();
    }

    @Override
    public void getReport(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        ReportResponse report = reportStore.get(request.getReportId());
        if (report == null) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            return;
        }
        responseObserver.onNext(report);
        responseObserver.onCompleted();
    }
}
