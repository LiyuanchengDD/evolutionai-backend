package com.example.grpcdemo.web.dto;

import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckResponse;

public record HealthStatusResponse(
        boolean ok,
        String status,
        Integer statusCode,
        String message) {

    public static HealthStatusResponse fromGrpcResponse(HealthCheckResponse response) {
        return new HealthStatusResponse(
                response.getStatus() == HealthCheckResponse.ServingStatus.SERVING,
                response.getStatus().name(),
                response.getStatus().getNumber(),
                null);
    }

    public static HealthStatusResponse fromGrpcException(StatusRuntimeException exception) {
        return new HealthStatusResponse(
                false,
                exception.getStatus().getCode().name(),
                exception.getStatus().getCode().value(),
                exception.getStatus().getDescription());
    }
}
