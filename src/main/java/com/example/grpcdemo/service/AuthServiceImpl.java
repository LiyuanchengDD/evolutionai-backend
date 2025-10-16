package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.proto.AuthServiceGrpc;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import com.example.grpcdemo.repository.UserAccountRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * gRPC implementation of the {@link AuthServiceGrpc.AuthServiceImplBase} contract.
 */
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registerUser(RegisterUserRequest request, StreamObserver<UserResponse> responseObserver) {
        String username = request.getUsername();
        String password = request.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Username and password are required")
                    .asRuntimeException());
            return;
        }

        username = username.trim();

        if (userRepository.findByEmailIgnoreCase(username).isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("User already exists")
                    .asRuntimeException());
            return;
        }

        String normalizedRole = normalizeRole(request.getRole());
        String encodedPassword = passwordEncoder.encode(password);

        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserId(UUID.randomUUID().toString());
        entity.setEmail(username);
        entity.setPasswordHash(encodedPassword);
        entity.setRole(normalizedRole);
        entity.setLastLoginAt(Instant.now());

        userRepository.save(entity);

        UserResponse response = buildUserResponse(entity, normalizedRole, username);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void loginUser(LoginRequest request, StreamObserver<UserResponse> responseObserver) {
        String username = request.getUsername();
        String password = request.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Username and password are required")
                    .asRuntimeException());
            return;
        }

        username = username.trim();

        userRepository.findByEmailIgnoreCase(username)
                .filter(entity -> rolesMatch(entity.getRole(), request.getRole()))
                .filter(entity -> passwordEncoder.matches(password, entity.getPasswordHash()))
                .ifPresentOrElse(entity -> {
                    entity.setLastLoginAt(Instant.now());
                    userRepository.save(entity);
                    UserResponse response = buildUserResponse(entity, entity.getRole(), username);
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }, () -> responseObserver.onError(Status.UNAUTHENTICATED
                        .withDescription("Invalid username, password, or role")
                        .asRuntimeException()));
    }

    private UserResponse buildUserResponse(UserAccountEntity entity, String role, String username) {
        return UserResponse.newBuilder()
                .setUserId(entity.getUserId())
                .setUsername(username)
                .setRole(role)
                .setAccessToken(generateAccessToken())
                .setRefreshToken("")
                .setEmail(entity.getEmail())
                .build();
    }

    private boolean rolesMatch(String storedRole, String providedRole) {
        return normalizeRole(storedRole).equals(normalizeRole(providedRole));
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "user";
        }
        return role.trim().toLowerCase(Locale.US);
    }

    private String generateAccessToken() {
        return UUID.randomUUID().toString();
    }
}
