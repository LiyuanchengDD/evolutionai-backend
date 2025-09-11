package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.ReportEntity;
import com.example.grpcdemo.proto.GenerateReportRequest;
import com.example.grpcdemo.proto.GetReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import com.example.grpcdemo.repository.ReportRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReportServiceIntegrationTest {

    @Autowired
    private ReportServiceImpl service;

    @Autowired
    private ReportRepository repository;

    @MockBean
    private AiEvaluationClient aiEvaluationClient;

    @Test
    void generateReport_persistsToDatabase() {
        when(aiEvaluationClient.evaluate("int1"))
                .thenReturn(new EvaluationResult("content", 5.0f, "nice"));

        TestObserver observer = new TestObserver();
        service.generateReport(GenerateReportRequest.newBuilder().setInterviewId("int1").build(), observer);

        assertNotNull(observer.value);
        Optional<ReportEntity> entity = repository.findById(observer.value.getReportId());
        assertTrue(entity.isPresent());
        assertEquals("content", entity.get().getContent());

        TestObserver getObserver = new TestObserver();
        service.getReport(GetReportRequest.newBuilder().setReportId(observer.value.getReportId()).build(), getObserver);
        assertEquals("content", getObserver.value.getContent());
    }

    @Test
    void getReport_notFoundReturnsError() {
        TestObserver observer = new TestObserver();
        service.getReport(GetReportRequest.newBuilder().setReportId("missing").build(), observer);
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
        }
    }
}

