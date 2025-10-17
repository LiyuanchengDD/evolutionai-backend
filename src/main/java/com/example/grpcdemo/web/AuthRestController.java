package com.example.grpcdemo.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.grpcdemo.proto.AuthServiceGrpc;
import com.example.grpcdemo.proto.LoginRequest;
import com.example.grpcdemo.proto.RegisterUserRequest;
import com.example.grpcdemo.proto.UserResponse;

import net.devh.boot.grpc.client.inject.GrpcClient;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    @GrpcClient("usersvc")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    @PostMapping("/login")
    public UserResponseDto login(@RequestBody LoginDto in) {
        LoginRequest request = LoginRequest.newBuilder()
                .setUsername(nvl(in.username()))
                .setPassword(nvl(in.password()))
                .setRole(nvl(in.role()))
                .build();
        UserResponse response = authStub.loginUser(request);
        return UserResponseDto.from(response);
    }

    @PostMapping("/register")
    public UserResponseDto register(@RequestBody RegisterDto in) {
        RegisterUserRequest request = RegisterUserRequest.newBuilder()
                .setUsername(nvl(in.username()))
                .setPassword(nvl(in.password()))
                .setRole(nvl(in.role()))
                .setEmail(nvl(in.email()))
                .build();
        UserResponse response = authStub.registerUser(request);
        return UserResponseDto.from(response);
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}

record LoginDto(String username, String password, String role) {
}

record RegisterDto(String username, String password, String role, String email) {
}

record UserResponseDto(String user_id, String username, String role, String access_token,
        String refresh_token, String email) {

    static UserResponseDto from(UserResponse response) {
        return new UserResponseDto(response.getUserId(), response.getUsername(), response.getRole(),
                response.getAccessToken(), response.getRefreshToken(), response.getEmail());
    }
}
