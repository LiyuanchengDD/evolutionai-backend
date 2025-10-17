package com.example.grpcdemo.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GrpcClient("usersvc")
    private HealthGrpc.HealthBlockingStub healthStub;

    @GetMapping("/health")
    public Map<String, String> health() {
        HealthCheckRequest request = HealthCheckRequest.newBuilder().setService("").build();
        var response = healthStub.check(request);
        return Map.of("status", response.getStatus().name());
    }
}
