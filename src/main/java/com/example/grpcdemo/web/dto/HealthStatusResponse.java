package com.example.grpcdemo.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckResponse;

@JsonInclude(Include.NON_NULL)
public record HealthStatusResponse(
        boolean ok,
        Integer status,
        String statusText,
        Boolean channelReady,
        String error) {

    public static HealthStatusResponse fromGrpcResponse(HealthCheckResponse response) {
        boolean serving = response.getStatus() == HealthCheckResponse.ServingStatus.SERVING;
        return new HealthStatusResponse(
                serving,
                response.getStatus().getNumber(),
                response.getStatus().name(),
                serving ? Boolean.TRUE : null,
                null);
    }

    public static HealthStatusResponse channelReadyResponse() {
        return new HealthStatusResponse(true, null, null, Boolean.TRUE, null);
    }

    public static HealthStatusResponse fromGrpcException(StatusRuntimeException exception) {
        return new HealthStatusResponse(
                false,
                exception.getStatus().getCode().value(),
                exception.getStatus().getCode().name(),
                null,
                resolveMessage(exception.getStatus().getDescription(), exception.getStatus().getCode().name()));
    }

    public static HealthStatusResponse fromFailure(Throwable exception) {
        return new HealthStatusResponse(false, null, null, null, resolveMessage(exception.getMessage(),
                exception.getClass().getSimpleName()));
    }

    private static String resolveMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
