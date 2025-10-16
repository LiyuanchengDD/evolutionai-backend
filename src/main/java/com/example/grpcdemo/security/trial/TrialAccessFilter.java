package com.example.grpcdemo.security.trial;

import com.example.grpcdemo.controller.dto.ErrorResponse;
import com.example.grpcdemo.entity.UserKind;
import com.example.grpcdemo.entity.UserProfileEntity;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.security.UserAuthenticationToken;
import com.example.grpcdemo.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TrialAccessFilter extends OncePerRequestFilter {

    private final TrialService trialService;
    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;
    private final List<String> guardedPaths = List.of("/api/enterprise/**");
    private final PathMatcher matcher = new AntPathMatcher();

    public TrialAccessFilter(TrialService trialService,
                             ObjectMapper objectMapper,
                             UserProfileService userProfileService) {
        this.trialService = trialService;
        this.objectMapper = objectMapper;
        this.userProfileService = userProfileService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof UserAuthenticationToken token)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser user = token.getPrincipal();
        UUID userId;
        try {
            userId = UUID.fromString(user.userId());
        } catch (IllegalArgumentException ex) {
            writeError(response, TrialErrorCode.NOT_ENTERPRISE_USER);
            return;
        }

        Optional<UserProfileEntity> profileOpt = userProfileService.findById(userId);
        if (profileOpt.isEmpty()) {
            writeError(response, TrialErrorCode.NOT_ENTERPRISE_USER);
            return;
        }

        UserProfileEntity profile = profileOpt.get();
        if (profile.getKind() != UserKind.ENTERPRISE) {
            writeError(response, TrialErrorCode.NOT_ENTERPRISE_USER);
            return;
        }
        if (profile.getCompanyId() == null) {
            writeError(response, TrialErrorCode.ENTERPRISE_PROFILE_INCOMPLETE);
            return;
        }

        TrialStatus status = trialService.evaluate(profile);
        if (status.status() == TrialStatusCode.NOT_SENT) {
            writeError(response, TrialErrorCode.TRIAL_INVITE_NOT_SENT);
            return;
        }
        if (status.status() == TrialStatusCode.EXPIRED) {
            writeError(response, TrialErrorCode.TRIAL_INVITE_EXPIRED);
            return;
        }

        if (status.status() == TrialStatusCode.VALID) {
            status = trialService.markRedeemed(profile);
        }

        request.setAttribute(TrialStatus.REQUEST_ATTRIBUTE, status);
        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : guardedPaths) {
            if (matcher.match(pattern, path)) {
                return false;
            }
        }
        return true;
    }

    private void writeError(HttpServletResponse response, TrialErrorCode errorCode) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        if (errorCode.getHttpStatus() != HttpStatus.FORBIDDEN) {
            response.setStatus(errorCode.getHttpStatus().value());
        }
        ErrorResponse body = new ErrorResponse(errorCode.name(), errorCode.getDefaultMessage());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

