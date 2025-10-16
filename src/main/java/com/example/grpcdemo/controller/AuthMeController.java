package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.AuthMeResponse;
import com.example.grpcdemo.controller.dto.UpdateUserKindRequest;
import com.example.grpcdemo.entity.UserKind;
import com.example.grpcdemo.entity.UserProfileEntity;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.security.trial.TrialService;
import com.example.grpcdemo.security.trial.TrialStatus;
import com.example.grpcdemo.security.trial.TrialStatusCode;
import com.example.grpcdemo.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

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
        return buildResponse(user, request);
    }

    @PutMapping("/kind")
    public AuthMeResponse updateKind(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestBody @Valid UpdateUserKindRequest request,
                                     HttpServletRequest httpRequest) {
        UUID userId = requireUserId(user.userId());
        UserKind kind = parseKind(request.getKind());
        userProfileService.upsertKind(userId, kind);
        return buildResponse(user, httpRequest);
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

    private UUID requireUserId(String raw) {
        UUID userId = safeParse(raw);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的用户 ID");
        }
        return userId;
    }

    private UserKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind 不能为空");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return UserKind.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind 仅支持 individual 或 enterprise");
        }
    }

    private AuthMeResponse buildResponse(AuthenticatedUser user, HttpServletRequest request) {
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
}

