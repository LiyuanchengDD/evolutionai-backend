package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.TrialApplicationAdminResponse;
import com.example.grpcdemo.controller.dto.TrialReviewRequest;
import com.example.grpcdemo.controller.dto.TrialSendRequest;
import com.example.grpcdemo.controller.dto.TrialSendResponse;
import com.example.grpcdemo.entity.ReviewStatus;
import com.example.grpcdemo.entity.UserProfileEntity;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.security.trial.TrialErrorCode;
import com.example.grpcdemo.security.trial.TrialException;
import com.example.grpcdemo.security.trial.TrialService;
import com.example.grpcdemo.security.trial.TrialStatus;
import com.example.grpcdemo.service.TrialApplicationService;
import com.example.grpcdemo.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/admin/trial")
public class TrialAdminController {

    private final TrialApplicationService trialApplicationService;
    private final TrialService trialService;
    private final UserProfileService userProfileService;

    public TrialAdminController(TrialApplicationService trialApplicationService,
                                TrialService trialService,
                                UserProfileService userProfileService) {
        this.trialApplicationService = trialApplicationService;
        this.trialService = trialService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/applications")
    public List<TrialApplicationAdminResponse> listApplications(Authentication authentication,
                                                                @RequestParam(name = "status", defaultValue = "PENDING") String status) {
        requireAdmin(authentication);
        ReviewStatus reviewStatus;
        try {
            reviewStatus = ReviewStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的状态: " + status);
        }
        return trialApplicationService.findByStatus(reviewStatus)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/review/{id}")
    public TrialApplicationAdminResponse review(Authentication authentication,
                                                @AuthenticationPrincipal AuthenticatedUser reviewer,
                                                @PathVariable("id") String id,
                                                @RequestBody @Valid TrialReviewRequest request) {
        requireAdmin(authentication);
        UUID applicationId = parseUuid(id, "申请 ID 无效");
        UUID reviewerId = parseUuid(reviewer.userId(), "用户 ID 无效");
        var entity = trialApplicationService.review(applicationId, request.getApprove(), reviewerId, request.getNote());
        return toResponse(entity);
    }

    @PostMapping("/send")
    public TrialSendResponse send(Authentication authentication,
                                  @RequestBody @Valid TrialSendRequest request) {
        requireAdmin(authentication);
        SendContext context = resolveSendContext(request);
        TrialService.TrialSendResult result = trialService.sendInvitation(context.companyId(), context.email());
        TrialStatus status = result.status();
        return new TrialSendResponse(
                status.status().getResponseValue(),
                status.sentAt() != null ? status.sentAt().toString() : null,
                status.expiresAt() != null ? status.expiresAt().toString() : null,
                result.code(),
                result.newlyCreated()
        );
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可操作");
        }
        boolean admin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可操作");
        }
    }

    private SendContext resolveSendContext(TrialSendRequest request) {
        if (request.getApplicationId() != null && !request.getApplicationId().isBlank()) {
            UUID applicationId = parseUuid(request.getApplicationId(), "申请 ID 无效");
            var application = trialApplicationService.findById(applicationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "试用申请不存在"));
            if (application.getStatus() != ReviewStatus.APPROVED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申请尚未通过审批");
            }
            UUID companyId = null;
            if (request.getCompanyId() != null && !request.getCompanyId().isBlank()) {
                companyId = parseUuid(request.getCompanyId(), "企业 ID 无效");
            }
            if (companyId == null) {
                companyId = userProfileService.findById(application.getApplicantUserId())
                        .map(UserProfileEntity::getCompanyId)
                        .orElse(null);
            }
            if (companyId == null) {
                throw new TrialException(TrialErrorCode.ENTERPRISE_PROFILE_INCOMPLETE);
            }
            return new SendContext(companyId, application.getContactEmail().toLowerCase(Locale.ROOT));
        }

        if (request.getCompanyId() == null || request.getCompanyId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "企业 ID 不能为空");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱不能为空");
        }
        UUID companyId = parseUuid(request.getCompanyId(), "企业 ID 无效");
        return new SendContext(companyId, request.getEmail().toLowerCase(Locale.ROOT));
    }

    private TrialApplicationAdminResponse toResponse(com.example.grpcdemo.entity.TrialApplicationEntity entity) {
        return new TrialApplicationAdminResponse(
                entity.getId().toString(),
                entity.getApplicantUserId().toString(),
                entity.getCompanyName(),
                entity.getContactEmail(),
                entity.getReason(),
                entity.getStatus().name(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().toString() : null,
                entity.getReviewedAt() != null ? entity.getReviewedAt().toString() : null,
                entity.getNote(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }

    private UUID parseUuid(String raw, String message) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private record SendContext(UUID companyId, String email) {
    }
}
