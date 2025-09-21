package com.example.grpcdemo.service;

import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;
import com.example.grpcdemo.repository.UserAccountRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private UserAccountRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserAccountRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AuthServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void registerUser_persistsHashedPassword() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TestObserver observer = new TestObserver();

        RegisterUserRequest request = RegisterUserRequest.newBuilder()
                .setUsername("alice")
                .setPassword("pa55w0rd")
                .setRole("ADMIN")
                .build();
        service.registerUser(request, observer);

        assertNull(observer.error);
        assertNotNull(observer.value);
        assertEquals("alice", observer.value.getUsername());
        ArgumentCaptor<UserAccountEntity> captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userRepository).save(captor.capture());
        UserAccountEntity saved = captor.getValue();
        assertEquals(observer.value.getUserId(), saved.getUserId());
        assertEquals("alice", saved.getUsername());
        assertEquals("ADMIN", saved.getRole());
        assertNotEquals("pa55w0rd", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("pa55w0rd", saved.getPasswordHash()));
    }

    @Test
    void registerUser_rejectsDuplicateUsername() {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(new UserAccountEntity("existing-id", "alice", "hash", "ADMIN")));
        TestObserver observer = new TestObserver();

        RegisterUserRequest request = RegisterUserRequest.newBuilder()
                .setUsername("alice")
                .setPassword("pa55w0rd")
                .setRole("ADMIN")
                .build();
        service.registerUser(request, observer);

        assertNull(observer.value);
        assertNotNull(observer.error);
        assertEquals(Status.Code.ALREADY_EXISTS, Status.fromThrowable(observer.error).getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginUser_returnsTokenWhenPasswordMatches() {
        String hashed = passwordEncoder.encode("secret");
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(new UserAccountEntity("id-1", "alice", hashed, "ADMIN")));
        TestObserver observer = new TestObserver();

        LoginRequest request = LoginRequest.newBuilder()
                .setUsername("alice")
                .setPassword("secret")
                .build();
        service.loginUser(request, observer);

        assertNull(observer.error);
        assertNotNull(observer.value);
        assertEquals("id-1", observer.value.getUserId());
        assertEquals("alice", observer.value.getUsername());
        assertEquals("ADMIN", observer.value.getRole());
        assertFalse(observer.value.getAccessToken().isEmpty());
    }

    @Test
    void loginUser_returnsUnauthenticatedWhenPasswordDoesNotMatch() {
        String hashed = passwordEncoder.encode("right-password");
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(new UserAccountEntity("id-1", "alice", hashed, "ADMIN")));
        TestObserver observer = new TestObserver();

        LoginRequest request = LoginRequest.newBuilder()
                .setUsername("alice")
                .setPassword("wrong")
                .build();
        service.loginUser(request, observer);

        assertNull(observer.value);
        assertNotNull(observer.error);
        assertEquals(Status.Code.UNAUTHENTICATED, Status.fromThrowable(observer.error).getCode());
    }

    private static class TestObserver implements StreamObserver<UserResponse> {
        private UserResponse value;
        private Throwable error;

        @Override
        public void onNext(UserResponse value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            // no-op
        }
    }
}
