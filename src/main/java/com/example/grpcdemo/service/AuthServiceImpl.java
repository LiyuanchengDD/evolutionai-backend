package com.example.grpcdemo.service;

import com.example.grpcdemo.auth.AuthErrorCode;
import com.example.grpcdemo.auth.AuthException;
import com.example.grpcdemo.auth.AuthManager;
import com.example.grpcdemo.auth.AuthRole;
import com.example.grpcdemo.proto.AuthServiceGrpc;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import com.example.grpcdemo.proto.VerificationCodeRequest;
import com.example.grpcdemo.proto.VerificationCodeResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthManager authManager;

    public AuthServiceImpl(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public void requestVerificationCode(VerificationCodeRequest request, StreamObserver<VerificationCodeResponse> responseObserver) {
        try {
            AuthRole role = AuthRole.fromGrpcValue(request.getRole());
            AuthManager.VerificationResult result = authManager.requestVerificationCode(request.getEmail(), role);
            VerificationCodeResponse response = VerificationCodeResponse.newBuilder()
                    .setRequestId(result.requestId())
                    .setExpiresInSeconds(result.expiresInSeconds())
                    .setVerificationCode(result.verificationCode())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (AuthException e) {
            handleAuthException(responseObserver, e);
        } catch (Exception e) {
            log.error("Unexpected error in requestVerificationCode", e);
            responseObserver.onError(AuthErrorCode.INTERNAL_ERROR.getStatus().withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void registerUser(RegisterUserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            AuthRole role = AuthRole.fromGrpcValue(request.getRole());
            AuthManager.AuthSession session = authManager.register(request.getEmail(), request.getPassword(), request.getVerificationCode(), role);
            responseObserver.onNext(toUserResponse(session));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (AuthException e) {
            handleAuthException(responseObserver, e);
        } catch (Exception e) {
            log.error("Unexpected error in registerUser", e);
            responseObserver.onError(AuthErrorCode.INTERNAL_ERROR.getStatus().withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void loginUser(LoginRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            AuthRole role = AuthRole.fromGrpcValue(request.getRole());
            AuthManager.AuthSession session = authManager.login(request.getEmail(), request.getPassword(), role);
            responseObserver.onNext(toUserResponse(session));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (AuthException e) {
            handleAuthException(responseObserver, e);
        } catch (Exception e) {
            log.error("Unexpected error in loginUser", e);
            responseObserver.onError(AuthErrorCode.INTERNAL_ERROR.getStatus().withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private UserResponse toUserResponse(AuthManager.AuthSession session) {
        return UserResponse.newBuilder()
                .setUserId(session.userId())
                .setEmail(session.email())
                .setRole(session.role().grpcValue())
                .setAccessToken(session.accessToken())
                .setRefreshToken(session.refreshToken())
                .build();
    }

    private void handleAuthException(StreamObserver<?> responseObserver, AuthException exception) {
        responseObserver.onError(exception.getErrorCode().getStatus().withDescription(exception.getMessage()).asRuntimeException());
    }
}
