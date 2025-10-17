package com.example.grpcdemo.gateway;

import org.springframework.stereotype.Service;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;

@Service
public class HealthGatewayService {

    private final GatewayGrpcClientFactory grpcClientFactory;

    public HealthGatewayService(GatewayGrpcClientFactory grpcClientFactory) {
        this.grpcClientFactory = grpcClientFactory;
    }

    public HealthCheckResponse check(String serviceName) {
        var request = HealthCheckRequest.newBuilder()
                .setService(serviceName == null ? "" : serviceName)
                .build();
        HealthGrpc.HealthBlockingStub stub = grpcClientFactory.withChannel(HealthGrpc::newBlockingStub);
        return stub.check(request);
    }
}
