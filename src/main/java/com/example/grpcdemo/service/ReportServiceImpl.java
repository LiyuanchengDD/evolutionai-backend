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

@GrpcService
public class ReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private final ReportRepository reportRepository;
    private final ReportGenerator reportGenerator;

    public ReportServiceImpl(ReportRepository reportRepository, ReportGenerator reportGenerator) {
        this.reportRepository = reportRepository;
        this.reportGenerator = reportGenerator;
    }

    @Override
    public void generateReport(GenerateReportRequest request,
                               StreamObserver<ReportResponse> responseObserver) {

    }

    @Override
    public void getReport(GetReportRequest request,
                          StreamObserver<ReportResponse> responseObserver) {

        reportRepository.findById(request.getReportId())
                .map(this::toResponse)
                .ifPresentOrElse(response -> {
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }, () -> responseObserver.onError(
                        Status.NOT_FOUND.withDescription("Report not found").asRuntimeException()));
    }

    private ReportResponse toResponse(ReportEntity entity) {
        return ReportResponse.newBuilder()
                .setReportId(entity.getReportId())
                .setInterviewId(entity.getInterviewId())
                .setContent(entity.getContent())
                .setScore(entity.getScore())
                .setEvaluatorComment(entity.getEvaluatorComment())
                .setCreatedAt(entity.getCreatedAt())
                .build();
    }
}
