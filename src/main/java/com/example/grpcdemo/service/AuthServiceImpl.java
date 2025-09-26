package com.example.grpcdemo.service;

import com.example.grpcdemo.auth.JwtUtil;
import com.example.grpcdemo.entity.UserAccountEntity;
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
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String role = request.getRole().trim();
        String normalizedRole = role.toUpperCase(Locale.ROOT);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (username.isEmpty() || password.isEmpty() || role.isEmpty() || email.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Username, password, role and email are required")
                    .asRuntimeException());
            return;
        }

        Optional<UserAccountEntity> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("User already exists")
                    .asRuntimeException());
            return;
        }

        Optional<UserAccountEntity> emailMatch = userRepository.findByEmail(email);
        if (emailMatch.isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("Email already registered")
                    .asRuntimeException());
            return;
        }

        String userId = UUID.randomUUID().toString();
        String hashedPassword = passwordEncoder.encode(password);
        UserAccountEntity entity = new UserAccountEntity(userId, username, email, hashedPassword, normalizedRole);
        userRepository.save(entity);
        log.info("Registered new user: username={}, email={}, role={}", username, email, normalizedRole);

        responseObserver.onNext(toResponse(entity));
        responseObserver.onCompleted();
    }

    @Override
    public void loginUser(LoginRequest request, StreamObserver<UserResponse> responseObserver) {
        String identifier = request.getUsername().trim();
        String password = request.getPassword();

        if (identifier.isEmpty() || password.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Username/email and password are required")
                    .asRuntimeException());
            return;
        }

        String lookupKey = identifier.contains("@")
                ? identifier.toLowerCase(Locale.ROOT)
                : identifier;
        Optional<UserAccountEntity> existing = identifier.contains("@")
                ? userRepository.findByEmail(lookupKey)
                : userRepository.findByUsername(lookupKey);
        if (existing.isEmpty() || !passwordEncoder.matches(password, existing.get().getPasswordHash())) {
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("Invalid username or password")
                    .asRuntimeException());
            return;
        }

        responseObserver.onNext(toResponse(existing.get()));
        responseObserver.onCompleted();
    }

    private UserResponse toResponse(UserAccountEntity entity) {
        String accessToken = JwtUtil.generateToken(entity.getUserId(), entity.getUsername(), entity.getRole());
        String refreshToken = UUID.randomUUID().toString();
        return UserResponse.newBuilder()
                .setUserId(entity.getUserId())
                .setUsername(entity.getUsername() != null ? entity.getUsername() : "")
                .setRole(entity.getRole() != null ? entity.getRole() : "")
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setEmail(entity.getEmail() != null ? entity.getEmail() : "")
                .build();
    }
}
