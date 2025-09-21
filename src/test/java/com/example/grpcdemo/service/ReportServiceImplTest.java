package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.proto.GenerateReportRequest;
import com.example.grpcdemo.proto.GetReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import com.example.grpcdemo.repository.ReportRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReportServiceImplTest {

    private ReportRepository reportRepository;
    private AiEvaluationClient aiEvaluationClient;
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        aiEvaluationClient = Mockito.mock(AiEvaluationClient.class);
        service = new ReportServiceImpl(reportRepository, aiEvaluationClient);
    }

    @Test
    void generateReport_persistsEntity() {
        when(aiEvaluationClient.evaluate("int1"))
                .thenReturn(new EvaluationResult("content", 4.5f, "good"));
        TestObserver observer = new TestObserver();

        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("int1").build(), observer);

        assertNull(observer.error);
        assertNotNull(observer.value);
        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportRepository).save(captor.capture());
        ReportEntity saved = captor.getValue();
        assertEquals(observer.value.getReportId(), saved.getReportId());
        assertEquals("int1", saved.getInterviewId());
        assertEquals("content", saved.getContent());
        assertEquals(4.5f, saved.getScore());
        assertEquals("good", saved.getEvaluatorComment());
    }

    @Test
    void generateReport_handlesEvaluationFailure() {
        when(aiEvaluationClient.evaluate(anyString())).thenThrow(new RuntimeException("fail"));
        TestObserver observer = new TestObserver();

        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("int1").build(), observer);

        assertNotNull(observer.error);
        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(observer.error).getCode());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void getReport_notFoundReturnsError() {
        when(reportRepository.findById("r1")).thenReturn(Optional.empty());
        TestObserver observer = new TestObserver();

        service.getReport(GetReportRequest.newBuilder().setReportId("r1").build(), observer);

        assertNull(observer.value);
        assertNotNull(observer.error);
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(observer.error).getCode());
    }

    private static class TestObserver implements StreamObserver<ReportResponse> {
        ReportResponse value;
        Throwable error;

        @Override
        public void onNext(ReportResponse value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            // no-op
        }
    }
}

