package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.NotificationServiceGrpc;
import com.example.grpcdemo.proto.SendInvitationRequest;
import com.example.grpcdemo.proto.SendInvitationResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@GrpcService
public class NotificationServiceImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendInvitation(SendInvitationRequest request, StreamObserver<SendInvitationResponse> responseObserver) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getEmail());
            message.setSubject(request.getSubject());
            message.setText(request.getContent());

            mailSender.send(message);

            SendInvitationResponse response = SendInvitationResponse.newBuilder()
                    .setSuccess(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Failed to send invitation email to {}", request.getEmail(), e);
            responseObserver.onError(Status.INTERNAL.asRuntimeException());
        }
    }
}

