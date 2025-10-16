package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.TrialApplyRequest;
import com.example.grpcdemo.controller.dto.TrialApplyResponse;
import com.example.grpcdemo.controller.dto.TrialVerifyRequest;
import com.example.grpcdemo.controller.dto.TrialVerifyResponse;
import com.example.grpcdemo.entity.UserKind;
import com.example.grpcdemo.entity.UserProfileEntity;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.security.trial.TrialErrorCode;
import com.example.grpcdemo.security.trial.TrialException;
import com.example.grpcdemo.security.trial.TrialService;
import com.example.grpcdemo.security.trial.TrialStatus;
import com.example.grpcdemo.service.TrialApplicationService;
import com.example.grpcdemo.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/trial")
public class TrialController {

    private final TrialApplicationService trialApplicationService;
    private final TrialService trialService;
    private final UserProfileService userProfileService;

    public TrialController(TrialApplicationService trialApplicationService,
                           TrialService trialService,
                           UserProfileService userProfileService) {
        this.trialApplicationService = trialApplicationService;
        this.trialService = trialService;
        this.userProfileService = userProfileService;
    }

    @PostMapping("/apply")
    public TrialApplyResponse apply(@AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestBody @Valid TrialApplyRequest request) {
        UUID userId = parseUserId(user.userId());
        String companyName = request.getCompanyName().trim();
        String email = request.getContactEmail().toLowerCase(Locale.ROOT);
        String reason = request.getReason();
        if (reason != null) {
            reason = reason.trim();
        }
        var entity = trialApplicationService.create(userId, companyName, email, reason);
        return new TrialApplyResponse(entity.getId().toString(), entity.getStatus().name());
    }

    @PostMapping("/verify")
    public TrialVerifyResponse verify(@AuthenticationPrincipal AuthenticatedUser user,
                                      @RequestBody @Valid TrialVerifyRequest request) {
        UUID userId = parseUserId(user.userId());
        UserProfileEntity profile = userProfileService.findById(userId)
                .orElseThrow(() -> new TrialException(TrialErrorCode.NOT_ENTERPRISE_USER));
        if (profile.getKind() != UserKind.ENTERPRISE || profile.getCompanyId() == null) {
            throw new TrialException(TrialErrorCode.ENTERPRISE_PROFILE_INCOMPLETE);
        }
        TrialStatus status = trialService.verifyCode(profile, request.getCode(), user.email());
        return new TrialVerifyResponse(
                status.status().getResponseValue(),
                status.sentAt() != null ? status.sentAt().toString() : null,
                status.expiresAt() != null ? status.expiresAt().toString() : null
        );
    }

    private UUID parseUserId(String raw) {
        if (raw == null) {
            throw new TrialException(TrialErrorCode.NOT_ENTERPRISE_USER);
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new TrialException(TrialErrorCode.NOT_ENTERPRISE_USER);
        }
    }
}
