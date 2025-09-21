package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.AuthServiceGrpc;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final Map<String, RegisteredUser> userStore = new ConcurrentHashMap<>();

    @Override
    public void registerUser(RegisterUserRequest request, StreamObserver<UserResponse> responseObserver) {
        String userId = UUID.randomUUID().toString();
        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        RegisteredUser user = new RegisteredUser(userId, request.getUsername(), hashedPassword, request.getRole());
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
        if (user != null && BCrypt.checkpw(request.getPassword(), user.hashedPassword())) {
            String token = JwtUtil.generateToken(user.userId(), user.username(), user.role());

            UserResponse response = UserResponse.newBuilder()
                    .setUserId(user.userId())
                    .setUsername(user.username())
                    .setRole(user.role())
                    .setAccessToken(token)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.UNAUTHENTICATED.asRuntimeException());
        }
    }

    private record RegisteredUser(String userId, String username, String hashedPassword, String role) {}
}
