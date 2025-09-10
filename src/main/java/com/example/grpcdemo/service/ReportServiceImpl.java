package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CreateReportRequest;
import com.example.grpcdemo.proto.ReportRequest;
import com.example.grpcdemo.proto.ReportResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of a simple reporting service. The gRPC specific
 * classes were removed so that the project can be compiled without external
 * dependencies. The service now exposes plain Java methods for creating and
 * retrieving reports.
 */
public class ReportServiceImpl {

    private final Map<String, ReportResponse> reports = new ConcurrentHashMap<>();

    public ReportResponse createReport(CreateReportRequest request) {
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
        return response;
    }

    public ReportResponse getReport(ReportRequest request) {
        return reports.getOrDefault(request.getReportId(),
                ReportResponse.newBuilder()
                        .setReportId(request.getReportId())
                        .setEvaluatorComment("NOT_FOUND")
                        .build());
    }
}

