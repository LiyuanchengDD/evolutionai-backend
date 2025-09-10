package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.CreateReportRequest;
import com.example.grpcdemo.proto.ReportRequest;
import com.example.grpcdemo.proto.ReportResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceImplTest {

    private static class TestObserver<T> implements StreamObserver<T> {
        T value;
        Throwable error;
        boolean completed;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }

    @Test
    void getReport_missingId_returnsNotFound() {
        ReportServiceImpl service = new ReportServiceImpl();
        TestObserver<ReportResponse> observer = new TestObserver<>();

        service.getReport(ReportRequest.newBuilder().setReportId("missing").build(), observer);

        assertNotNull(observer.error);
        assertTrue(observer.error instanceof StatusRuntimeException);
        assertEquals(Status.NOT_FOUND.getCode(), ((StatusRuntimeException) observer.error).getStatus().getCode());
    }

    @Test
    void getReport_existingId_returnsReport() {
        ReportServiceImpl service = new ReportServiceImpl();
        TestObserver<ReportResponse> createObserver = new TestObserver<>();
        service.createReport(CreateReportRequest.newBuilder().setContent("hello").build(), createObserver);
        String id = createObserver.value.getReportId();

        TestObserver<ReportResponse> observer = new TestObserver<>();
        service.getReport(ReportRequest.newBuilder().setReportId(id).build(), observer);

        assertNull(observer.error);
        assertTrue(observer.completed);
        assertEquals(id, observer.value.getReportId());
        assertEquals("hello", observer.value.getContent());
    }
}
