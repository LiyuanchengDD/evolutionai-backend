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
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportServiceImplTest {

    private ReportRepository reportRepository;
    private ReportGenerator reportGenerator;
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        reportGenerator = Mockito.mock(ReportGenerator.class);
        service = new ReportServiceImpl(reportRepository, reportGenerator);
    }

    @Test
    void generateReport_persistsEntity() {
        ReportEntity generated = new ReportEntity("r1", "int1", "content", 4.5f, "good", 5L);
        when(reportGenerator.generateAndStore("int1")).thenReturn(generated);
        TestObserver observer = new TestObserver();

        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("int1").build(), observer);

        assertNull(observer.error);
        assertNotNull(observer.value);
        assertEquals("r1", observer.value.getReportId());
        assertEquals("int1", observer.value.getInterviewId());
        assertEquals("content", observer.value.getContent());
        assertEquals(4.5f, observer.value.getScore());
        assertEquals("good", observer.value.getEvaluatorComment());
        verify(reportGenerator).generateAndStore("int1");
    }

    @Test
    void generateReport_handlesEvaluationFailure() {
        when(reportGenerator.generateAndStore("int1")).thenThrow(new RuntimeException("fail"));
        TestObserver observer = new TestObserver();

        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("int1").build(), observer);

        assertNotNull(observer.error);
        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(observer.error).getCode());
        verify(reportGenerator).generateAndStore("int1");
    }

    @Test
    void generateReport_missingInterviewReturnsNotFound() {
        when(reportGenerator.generateAndStore("missing")).thenThrow(new InterviewNotFoundException("missing"));
        TestObserver observer = new TestObserver();

        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("missing").build(), observer);

        assertNotNull(observer.error);
        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(observer.error).getCode());
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

    @Test
    void getReport_returnsPersistedReport() {
        ReportEntity entity = new ReportEntity("r2", "int2", "content", 3.5f, "great", 123L);
        when(reportRepository.findById("r2")).thenReturn(Optional.of(entity));
        TestObserver observer = new TestObserver();

        service.getReport(GetReportRequest.newBuilder().setReportId("r2").build(), observer);

        assertNotNull(observer.value);
        assertNull(observer.error);
        assertEquals("r2", observer.value.getReportId());
        assertEquals("int2", observer.value.getInterviewId());
        assertEquals("content", observer.value.getContent());
        assertEquals(3.5f, observer.value.getScore());
        assertEquals("great", observer.value.getEvaluatorComment());
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

