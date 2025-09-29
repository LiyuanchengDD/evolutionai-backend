package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.entity.UserAccountStatus;
import com.example.grpcdemo.proto.AuthServiceGrpc;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import com.example.grpcdemo.repository.UserAccountRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * gRPC authentication service that persists user accounts and issues JWT tokens.
 */
@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registerUser(RegisterUserRequest request, StreamObserver<UserResponse> responseObserver) {
        String email = request.getEmail().isBlank() ? request.getUsername().trim() : request.getEmail().trim();
        String password = request.getPassword();
        String role = request.getRole().trim();
        String normalizedRole = role.toLowerCase(Locale.ROOT);

        if (email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Email, password and role are required")
                    .asRuntimeException());
            return;
        }

        Optional<UserAccountEntity> existing = userRepository.findByEmailAndRole(email, normalizedRole);
        if (existing.isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("User already exists")
                    .asRuntimeException());
            return;
        }

        String userId = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(password);
        UserAccountEntity entity = new UserAccountEntity(userId, email, hashedPassword, normalizedRole, UserAccountStatus.ACTIVE);
        userRepository.save(entity);
        log.info("Registered new user: email={}, role={}", email, normalizedRole);

        responseObserver.onNext(toResponse(entity));
        responseObserver.onCompleted();
    }

    @Override
    public void loginUser(LoginRequest request, StreamObserver<UserResponse> responseObserver) {
        String email = request.getUsername().trim();
        String password = request.getPassword();
        String role = request.getRole().trim();
        String normalizedRole = role.toLowerCase(Locale.ROOT);

        if (email.isEmpty() || password.isEmpty() || role.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Email, password and role are required")
                    .asRuntimeException());
            return;
        }

        Optional<UserAccountEntity> existing = userRepository.findByEmailAndRole(email, normalizedRole);
        if (existing.isEmpty() || !passwordEncoder.matches(password, existing.get().getPasswordHash())) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Invalid email or password")
                    .asRuntimeException());
            return;
        }

        UserAccountEntity entity = existing.get();
        entity.setLastLoginAt(Instant.now());
        if (entity.getStatus() == UserAccountStatus.PENDING) {
            entity.setStatus(UserAccountStatus.ACTIVE);
        }
        userRepository.save(entity);

        responseObserver.onNext(toResponse(entity));
        responseObserver.onCompleted();
    }

    private UserResponse toResponse(UserAccountEntity entity) {
        String accessToken = JwtUtil.generateToken(entity.getUserId(), entity.getEmail(), entity.getRole());
        String refreshToken = UUID.randomUUID().toString();
        return UserResponse.newBuilder()
                .setUserId(entity.getUserId())
                .setUsername(entity.getEmail())
                .setRole(entity.getRole())
                .setEmail(entity.getEmail())
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .build();
    }
}
