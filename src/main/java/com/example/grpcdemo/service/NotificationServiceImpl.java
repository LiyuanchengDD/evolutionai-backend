package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.NotificationServiceGrpc;
import com.example.grpcdemo.proto.SendInvitationRequest;
import com.example.grpcdemo.proto.SendInvitationResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * gRPC service responsible for sending email invitations.
 */
@GrpcService
public class NotificationServiceImpl extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;

    public NotificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendInvitation(SendInvitationRequest request, StreamObserver<SendInvitationResponse> responseObserver) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getEmail());
            message.setSubject(request.getSubject());
            message.setText(request.getContent());
            mailSender.send(message);

            logger.info("Sent invitation email to {} with subject '{}'", request.getEmail(), request.getSubject());

            SendInvitationResponse response = SendInvitationResponse.newBuilder()
                    .setSuccess(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (MailException e) {
            String baseMessage = String.format("Failed to send invitation email to %s with subject '%s'",
                    request.getEmail(), request.getSubject());
            String detailedMessage = baseMessage;
            if (e.getMessage() != null && !e.getMessage().isBlank()) {
                detailedMessage = baseMessage + ". Cause: " + e.getMessage();
            }

            logger.error(detailedMessage, e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(detailedMessage)
                    .asRuntimeException());
        }
    }
}
