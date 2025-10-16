package com.example.grpcdemo.health;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.protobuf.services.ProtoReflectionService;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ReflectionService implements BindableService {

    private final BindableService delegate = ProtoReflectionService.newInstance();

    @Override
    public ServerServiceDefinition bindService() {
        return delegate.bindService();
    }
}

