package com.example.grpcdemo.gateway;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.grpcdemo.web.dto.HealthStatusResponse;

import io.grpc.ConnectivityState;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;

@Service
public class HealthGatewayService {

    private static final Duration HEALTH_DEADLINE = Duration.ofSeconds(3);
    private static final Duration CHANNEL_READY_TIMEOUT = Duration.ofSeconds(3);
    private static final Set<Status.Code> FALLBACK_STATUSES = EnumSet.of(Status.Code.UNIMPLEMENTED,
            Status.Code.NOT_FOUND);

    private final GatewayGrpcClientFactory grpcClientFactory;

    public HealthGatewayService(GatewayGrpcClientFactory grpcClientFactory) {
        this.grpcClientFactory = grpcClientFactory;
    }

    public HealthStatusResponse check(String serviceName) {
        var request = HealthCheckRequest.newBuilder()
                .setService(serviceName == null ? "" : serviceName)
                .build();
        HealthGrpc.HealthBlockingStub stub = grpcClientFactory.withChannel(HealthGrpc::newBlockingStub)
                .withWaitForReady()
                .withDeadlineAfter(HEALTH_DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        try {
            var response = stub.check(request);
            return HealthStatusResponse.fromGrpcResponse(response);
        } catch (StatusRuntimeException exception) {
            if (FALLBACK_STATUSES.contains(exception.getStatus().getCode())) {
                boolean ready = awaitChannelReady();
                if (ready) {
                    return HealthStatusResponse.channelReadyResponse();
                }
            }
            return HealthStatusResponse.fromGrpcException(exception);
        } catch (Exception exception) {
            return HealthStatusResponse.fromFailure(exception);
        }
    }

    private boolean awaitChannelReady() {
        var channel = grpcClientFactory.managedChannel();
        long deadline = System.nanoTime() + CHANNEL_READY_TIMEOUT.toNanos();
        ConnectivityState currentState = channel.getState(true);
        if (currentState == ConnectivityState.READY) {
            return true;
        }
        while (true) {
            if (currentState == ConnectivityState.READY) {
                return true;
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return channel.getState(false) == ConnectivityState.READY;
            }
            CountDownLatch latch = new CountDownLatch(1);
            channel.notifyWhenStateChanged(currentState, latch::countDown);
            try {
                if (!latch.await(remaining, TimeUnit.NANOSECONDS)) {
                    return channel.getState(false) == ConnectivityState.READY;
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return channel.getState(false) == ConnectivityState.READY;
            }
            currentState = channel.getState(false);
        }
    }
}
