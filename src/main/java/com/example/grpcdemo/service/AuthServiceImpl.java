package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.proto.AuthServiceGrpc;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import com.example.grpcdemo.repository.UserAccountRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registerUser(RegisterUserRequest request, StreamObserver<UserResponse> responseObserver) {
        userRepository.findByUsername(request.getUsername())
                .ifPresentOrElse(existing -> responseObserver.onError(
                                Status.ALREADY_EXISTS
                                        .withDescription("Username already registered")
                                        .asRuntimeException()),
                        () -> {
                            String userId = UUID.randomUUID().toString();
                            String hashedPassword = passwordEncoder.encode(request.getPassword());
                            UserAccountEntity user = new UserAccountEntity(userId, request.getUsername(), hashedPassword, request.getRole());
                            userRepository.save(user);

                            UserResponse response = UserResponse.newBuilder()
                                    .setUserId(userId)
                                    .setUsername(request.getUsername())
                                    .setRole(request.getRole())
                                    .build();
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        });
    }

    @Override
    public void loginUser(LoginRequest request, StreamObserver<UserResponse> responseObserver) {
        userRepository.findByUsername(request.getUsername())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .ifPresentOrElse(user -> {
                    String token = JwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());

                    UserResponse response = UserResponse.newBuilder()
                            .setUserId(user.getUserId())
                            .setUsername(user.getUsername())
                            .setRole(user.getRole())
                            .setAccessToken(token)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }, () -> responseObserver.onError(Status.UNAUTHENTICATED.asRuntimeException()));
    }
}
