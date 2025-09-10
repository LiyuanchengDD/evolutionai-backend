package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.NotificationServiceGrpc;
import com.example.grpcdemo.proto.SendInvitationRequest;
import com.example.grpcdemo.proto.SendInvitationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class NotificationServiceImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    @Override
    public void sendInvitation(SendInvitationRequest request, StreamObserver<SendInvitationResponse> responseObserver) {
        SendInvitationResponse response = SendInvitationResponse.newBuilder()
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}