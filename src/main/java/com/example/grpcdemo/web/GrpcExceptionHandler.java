package com.example.grpcdemo.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.grpc.StatusRuntimeException;

@RestControllerAdvice
public class GrpcExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, Object>> handle(StatusRuntimeException exception) {
        var status = exception.getStatus();
        int httpStatus = switch (status.getCode()) {
            case INVALID_ARGUMENT -> 400;
            case NOT_FOUND -> 404;
            case ALREADY_EXISTS -> 409;
            case PERMISSION_DENIED, UNAUTHENTICATED -> 401;
            case UNAVAILABLE, DEADLINE_EXCEEDED -> 503;
            default -> 500;
        };
        return ResponseEntity.status(httpStatus).body(Map.of(
                "code", status.getCode().name(),
                "message", String.valueOf(status.getDescription())));
    }
}
