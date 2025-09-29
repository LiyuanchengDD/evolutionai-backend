package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import com.example.grpcdemo.repository.UserAccountRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthServiceImplTest {

    @Autowired
    private UserAccountRepository userRepository;

    private AuthServiceImpl authService;

    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void loginUserReturnsTokenWhenCredentialsValid() {
        RegisterUserRequest registerRequest = RegisterUserRequest.newBuilder()
                .setUsername("valid-user")
                .setPassword("strong-password")
                .setRole("ADMIN")
                .build();

        TestStreamObserver<UserResponse> registerObserver = new TestStreamObserver<>();
        authService.registerUser(registerRequest, registerObserver);

        assertThat(registerObserver.getError()).isNull();
        assertThat(registerObserver.isCompleted()).isTrue();
        assertThat(registerObserver.getValues()).hasSize(1);

        UserResponse registered = registerObserver.getValues().get(0);
        assertThat(registered.getUserId()).isNotBlank();
        assertThat(registered.getUsername()).isEqualTo("valid-user");
        assertThat(registered.getRole()).isEqualTo("admin");

        userRepository.flush();
        UserAccountEntity persisted =
            userRepository.findById(registered.getUserId()).orElseThrow();

        assertThat(persisted.getPasswordHash()).isNotEqualTo("strong-password");
        assertThat(passwordEncoder.matches("strong-password", persisted.getPasswordHash())).isTrue();

        TestStreamObserver<UserResponse> loginObserver = new TestStreamObserver<>();
        LoginRequest loginRequest = LoginRequest.newBuilder()
                .setUsername("valid-user")
                .setPassword("strong-password")
                .setRole("ADMIN")
                .build();

        authService.loginUser(loginRequest, loginObserver);

        assertThat(loginObserver.getError()).isNull();
        assertThat(loginObserver.isCompleted()).isTrue();
        assertThat(loginObserver.getValues()).hasSize(1);

        UserResponse loginResponse = loginObserver.getValues().get(0);
        assertThat(loginResponse.getUserId()).isEqualTo(registered.getUserId());
        assertThat(loginResponse.getUsername()).isEqualTo("valid-user");
        assertThat(loginResponse.getRole()).isEqualTo("admin");
        assertThat(loginResponse.getAccessToken()).isNotBlank();
    }

    @Test
    void loginUserWithInvalidCredentialsReturnsUnauthenticated() {
        RegisterUserRequest registerRequest = RegisterUserRequest.newBuilder()
                .setUsername("invalid-user")
                .setPassword("correct-password")
                .setRole("COMPANY")
                .build();

        TestStreamObserver<UserResponse> registerObserver = new TestStreamObserver<>();
        authService.registerUser(registerRequest, registerObserver);
        assertThat(registerObserver.getError()).isNull();

        TestStreamObserver<UserResponse> loginObserver = new TestStreamObserver<>();
        LoginRequest loginRequest = LoginRequest.newBuilder()
                .setUsername("invalid-user")
                .setPassword("wrong-password")
                .setRole("COMPANY")
                .build();

        authService.loginUser(loginRequest, loginObserver);

        assertThat(loginObserver.getValues()).isEmpty();
        assertThat(loginObserver.isCompleted()).isFalse();
        assertThat(loginObserver.getError()).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException exception = (StatusRuntimeException) loginObserver.getError();
        assertThat(Status.fromThrowable(exception).getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
    }

    @Test
    void registerUserWithDuplicateUsernameReturnsAlreadyExists() {
        RegisterUserRequest registerRequest = RegisterUserRequest.newBuilder()
                .setUsername("duplicate-user")
                .setPassword("password")
                .setRole("ENGINEER")
                .build();

        TestStreamObserver<UserResponse> firstObserver = new TestStreamObserver<>();
        authService.registerUser(registerRequest, firstObserver);

        assertThat(firstObserver.getError()).isNull();
        assertThat(firstObserver.getValues()).hasSize(1);

        TestStreamObserver<UserResponse> duplicateObserver = new TestStreamObserver<>();
        authService.registerUser(registerRequest, duplicateObserver);

        assertThat(duplicateObserver.getValues()).isEmpty();
        assertThat(duplicateObserver.isCompleted()).isFalse();
        assertThat(duplicateObserver.getError()).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException exception = (StatusRuntimeException) duplicateObserver.getError();
        assertThat(Status.fromThrowable(exception).getCode()).isEqualTo(Status.ALREADY_EXISTS.getCode());
    }

    private static final class TestStreamObserver<T> implements StreamObserver<T> {
        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        List<T> getValues() {
            return values;
        }

        Throwable getError() {
            return error;
        }

        boolean isCompleted() {
            return completed;
        }
    }
}
