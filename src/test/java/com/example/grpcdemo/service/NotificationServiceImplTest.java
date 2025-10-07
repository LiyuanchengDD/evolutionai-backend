package com.example.grpcdemo.service;

import com.example.grpcdemo.proto.SendInvitationRequest;
import com.example.grpcdemo.proto.SendInvitationResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private StreamObserver<SendInvitationResponse> responseObserver;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private SendInvitationRequest request;

    @BeforeEach
    void setUp() {
        request = SendInvitationRequest.newBuilder()
                .setEmail("candidate@example.com")
                .setSubject("Interview Invitation")
                .setContent("Please join us for an interview.")
                .build();
    }

    @Test
    void sendInvitation_deliversSuccessResponseWhenMailSent() {
        notificationService.sendInvitation(request, responseObserver);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        ArgumentCaptor<SendInvitationResponse> responseCaptor = ArgumentCaptor.forClass(SendInvitationResponse.class);
        verify(mailSender).send(mailCaptor.capture());
        verify(responseObserver).onNext(responseCaptor.capture());
        assertTrue(responseCaptor.getValue().getSuccess());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        SimpleMailMessage sentMessage = mailCaptor.getValue();
        assertEquals("candidate@example.com", sentMessage.getTo()[0]);
        assertEquals("Interview Invitation", sentMessage.getSubject());
        assertEquals("Please join us for an interview.", sentMessage.getText());
    }

    @Test
    void sendInvitation_propagatesGrpcErrorWhenMailFails() {
        MailSendException mailException = new MailSendException("SMTP server not available");
        doThrow(mailException).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendInvitation(request, responseObserver);

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(responseObserver).onError(errorCaptor.capture());

        Throwable thrown = errorCaptor.getValue();
        assertTrue(thrown instanceof StatusRuntimeException);

        Status status = Status.fromThrowable(thrown);
        assertEquals(Status.Code.INTERNAL, status.getCode());
        assertEquals("Failed to send invitation email to candidate@example.com with subject 'Interview Invitation'. Cause: SMTP server not available",
                status.getDescription());
    }
}
