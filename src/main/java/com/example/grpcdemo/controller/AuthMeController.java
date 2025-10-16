package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.AuthMeResponse;
import com.example.grpcdemo.entity.UserKind;
import com.example.grpcdemo.entity.UserProfileEntity;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.security.trial.TrialService;
import com.example.grpcdemo.security.trial.TrialStatus;
import com.example.grpcdemo.security.trial.TrialStatusCode;
import com.example.grpcdemo.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthMeController {

    private final TrialService trialService;
    private final UserProfileService userProfileService;

    public AuthMeController(TrialService trialService, UserProfileService userProfileService) {
        this.trialService = trialService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal AuthenticatedUser user,
                             HttpServletRequest request) {
        TrialStatus status = (TrialStatus) request.getAttribute(TrialStatus.REQUEST_ATTRIBUTE);
        UUID userId = safeParse(user.userId());
        Optional<UserProfileEntity> profileOpt = userId != null ? userProfileService.findById(userId) : Optional.empty();
        UserKind kind = profileOpt.map(UserProfileEntity::getKind).orElse(UserKind.INDIVIDUAL);

        if (status == null) {
            status = profileOpt.map(trialService::evaluate)
                    .orElseGet(() -> trialService.evaluateByEmail(user.email()));
        }

        String statusValue = status != null ? status.status().getResponseValue() : TrialStatusCode.NOT_SENT.getResponseValue();
        String sentAt = status != null && status.sentAt() != null ? status.sentAt().toString() : null;
        String expiresAt = status != null && status.expiresAt() != null ? status.expiresAt().toString() : null;

        AuthMeResponse.Trial trialDto = new AuthMeResponse.Trial(statusValue, sentAt, expiresAt);
        return new AuthMeResponse(user.userId(), kind.name().toLowerCase(), trialDto);
    }

    private UUID safeParse(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

