package com.example.grpcdemo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grpcdemo.gateway.HealthGatewayService;
import com.example.grpcdemo.web.dto.HealthStatusResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.grpc.StatusRuntimeException;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthGatewayService healthGatewayService;

    public HealthController(HealthGatewayService healthGatewayService) {
        this.healthGatewayService = healthGatewayService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthStatusResponse> health() {
        try {
            var response = healthGatewayService.check("");
            return ResponseEntity.ok(HealthStatusResponse.fromGrpcResponse(response));
        } catch (StatusRuntimeException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(HealthStatusResponse.fromGrpcException(exception));
        }
    }
}
