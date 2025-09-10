package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.AuthServiceGrpc;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final Map<String, RegisteredUser> userStore = new ConcurrentHashMap<>();

    @Override
    public void registerUser(RegisterUserRequest request, StreamObserver<UserResponse> responseObserver) {
        String userId = UUID.randomUUID().toString();
        RegisteredUser user = new RegisteredUser(userId, request.getUsername(), request.getPassword(), request.getRole());
        userStore.put(request.getUsername(), user);

        UserResponse response = UserResponse.newBuilder()
                .setUserId(userId)
                .setUsername(request.getUsername())
                .setRole(request.getRole())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void loginUser(LoginRequest request, StreamObserver<UserResponse> responseObserver) {
        RegisteredUser user = userStore.get(request.getUsername());
        if (user != null && user.password().equals(request.getPassword())) {
            UserResponse response = UserResponse.newBuilder()
                    .setUserId(user.userId())
                    .setUsername(user.username())
                    .setRole(user.role())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onNext(UserResponse.newBuilder()
                    .setUserId("")
                    .setUsername("Invalid")
                    .setRole("Unknown")
                    .build());
            responseObserver.onCompleted();
        }
    }

    private record RegisteredUser(String userId, String username, String password, String role) {}
}