package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.proto.GenerateReportRequest;
import com.example.grpcdemo.proto.GetReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import com.example.grpcdemo.proto.ReportServiceGrpc;
import com.example.grpcdemo.repository.ReportRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC service that delegates report generation to the {@link ReportGenerator}
 * and persists generated reports using {@link ReportRepository}.
 */
@GrpcService
public class ReportServiceImpl extends ReportServiceGrpc.ReportServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportRepository reportRepository;
    private final ReportGenerator reportGenerator;

    public ReportServiceImpl(ReportRepository reportRepository, ReportGenerator reportGenerator) {
        this.reportRepository = reportRepository;
        this.reportGenerator = reportGenerator;
    }

    @Override
    public void generateReport(GenerateReportRequest request,
                               StreamObserver<ReportResponse> responseObserver) {
        String interviewId = request.getInterviewId();
        if (interviewId.isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Interview id must not be empty")
                    .asRuntimeException());
            return;
        }
        try {
            ReportEntity entity = reportGenerator.generateAndStore(interviewId);
            responseObserver.onNext(toResponse(entity));
            responseObserver.onCompleted();
        } catch (InterviewNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Failed to generate report for interview {}", interviewId, e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to generate report")
                    .asRuntimeException());
        }
    }

    @Override
    public void getReport(GetReportRequest request, StreamObserver<ReportResponse> responseObserver) {
        String reportId = request.getReportId();
        if (reportId.isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Report id must not be empty")
                    .asRuntimeException());
            return;
        }
        reportRepository.findById(reportId)
                .map(this::toResponse)
                .ifPresentOrElse(response -> {
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }, () -> responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Report not found")
                        .asRuntimeException()));
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
