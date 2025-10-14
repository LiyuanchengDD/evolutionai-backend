package com.example.grpcdemo.auth;

import com.example.grpcdemo.entity.TrialInvitationEntity;
import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.entity.UserAccountStatus;
import com.example.grpcdemo.repository.AuthVerificationCodeRepository;
import com.example.grpcdemo.repository.TrialInvitationRepository;
import com.example.grpcdemo.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthManagerTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private AuthVerificationCodeRepository verificationCodeRepository;

    @Mock
    private TrialInvitationRepository trialInvitationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationCodeSender verificationCodeSender;

    private Clock clock;

    private AuthManager authManager;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-05-01T00:00:00Z"), ZoneOffset.UTC);
        authManager = new AuthManager(
                userRepository,
                verificationCodeRepository,
                trialInvitationRepository,
                passwordEncoder,
                verificationCodeSender,
                new SecureRandom(),
                clock
        );
    }

    @Test
    void loginCompanyWithoutInvitationThrows() {
        String email = "user@example.com";
        UserAccountEntity user = new UserAccountEntity("id", email, "hash", AuthRole.COMPANY.alias(), UserAccountStatus.ACTIVE);
        when(userRepository.findByEmailAndRole(eq(email), eq(AuthRole.COMPANY.alias()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(trialInvitationRepository.findTopByEmailOrderBySentAtDesc(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authManager.login(email, "password", AuthRole.COMPANY))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.TRIAL_INVITE_NOT_SENT);
    }

    @Test
    void loginCompanyWithExpiredInvitationThrows() {
        String email = "expired@example.com";
        UserAccountEntity user = new UserAccountEntity("id", email, "hash", AuthRole.COMPANY.alias(), UserAccountStatus.ACTIVE);
        when(userRepository.findByEmailAndRole(eq(email), eq(AuthRole.COMPANY.alias()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        TrialInvitationEntity invitation = new TrialInvitationEntity();
        invitation.setInvitationId("inv1");
        invitation.setEmail(email);
        invitation.setSentAt(clock.instant().minus(Duration.ofDays(14)).minusSeconds(1));
        when(trialInvitationRepository.findTopByEmailOrderBySentAtDesc(email)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> authManager.login(email, "password", AuthRole.COMPANY))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getErrorCode())
                .isEqualTo(AuthErrorCode.TRIAL_INVITE_EXPIRED);
    }

    @Test
    void loginCompanyWithValidInvitationSucceeds() {
        String email = "valid@example.com";
        UserAccountEntity user = new UserAccountEntity("id", email, "hash", AuthRole.COMPANY.alias(), UserAccountStatus.ACTIVE);
        when(userRepository.findByEmailAndRole(eq(email), eq(AuthRole.COMPANY.alias()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(userRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrialInvitationEntity invitation = new TrialInvitationEntity();
        invitation.setInvitationId("inv2");
        invitation.setEmail(email);
        invitation.setSentAt(clock.instant().minus(Duration.ofDays(1)));
        when(trialInvitationRepository.findTopByEmailOrderBySentAtDesc(email)).thenReturn(Optional.of(invitation));

        AuthManager.AuthSession session = authManager.login(email, "password", AuthRole.COMPANY);

        assertThat(session.email()).isEqualTo(email);
        assertThat(session.role()).isEqualTo(AuthRole.COMPANY);
        assertThat(session.userId()).isEqualTo("id");
    }

    @Test
    void loginEngineerSkipsInvitationCheck() {
        String email = "engineer@example.com";
        UserAccountEntity user = new UserAccountEntity("id", email, "hash", AuthRole.ENGINEER.alias(), UserAccountStatus.ACTIVE);
        when(userRepository.findByEmailAndRole(eq(email), eq(AuthRole.ENGINEER.alias()))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(userRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthManager.AuthSession session = authManager.login(email, "password", AuthRole.ENGINEER);

        assertThat(session.role()).isEqualTo(AuthRole.ENGINEER);
    }
}
