package com.example.grpcdemo.gateway;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.grpc.Channel;
import net.devh.boot.grpc.client.inject.GrpcClient;

/**
 * Central place that exposes the shared gRPC channel used by the REST
 * gateway. Additional REST endpoints can obtain strongly typed stubs from
 * this factory when they need to talk to the underlying gRPC services.
 */
@Component
public class GatewayGrpcClientFactory {

    private final Channel channel;

    public GatewayGrpcClientFactory(@GrpcClient("usersvc") Channel channel) {
        this.channel = channel;
    }

    public Channel channel() {
        return channel;
    }

    public <T> T withChannel(Function<Channel, T> callback) {
        return callback.apply(channel);
    }
}
