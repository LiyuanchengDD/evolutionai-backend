package com.example.grpcdemo.health;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC health check endpoint that reports the overall serving status of the application.
 */
@GrpcService
public class HealthCheckService extends HealthGrpc.HealthImplBase {

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        respondHealthy(responseObserver);
    }

    @Override
    public void watch(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        respondHealthy(responseObserver);
    }

    private void respondHealthy(StreamObserver<HealthCheckResponse> responseObserver) {
        HealthCheckResponse response = HealthCheckResponse.newBuilder()
                .setStatus(ServingStatus.SERVING)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

