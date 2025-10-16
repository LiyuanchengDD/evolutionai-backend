package com.example.grpcdemo.security.trial;

import com.example.grpcdemo.controller.dto.ErrorResponse;
import com.example.grpcdemo.security.AppAuthProperties;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.security.UserAuthenticationToken;
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

@Component
public class TrialAccessFilter extends OncePerRequestFilter {

    private final TrialService trialService;
    private final ObjectMapper objectMapper;
    private final List<String> whitelist;
    private final PathMatcher matcher = new AntPathMatcher();

    public TrialAccessFilter(TrialService trialService,
                             ObjectMapper objectMapper,
                             AppAuthProperties authProperties) {
        this.trialService = trialService;
        this.objectMapper = objectMapper;
        if (authProperties.isDevOtpMode()) {
            this.whitelist = List.of("/public/**", "/health", "/auth/me", "/dev/auth/**");
        } else {
            this.whitelist = List.of("/public/**", "/health", "/auth/me");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isWhitelisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof UserAuthenticationToken token)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedUser user = token.getPrincipal();
        TrialStatus status = trialService.evaluate(user.email());
        request.setAttribute(TrialStatus.REQUEST_ATTRIBUTE, status);

        if (status.status() == TrialStatusCode.NOT_SENT) {
            writeError(response, "TRIAL_INVITE_NOT_SENT", "试用邀请尚未发送");
            return;
        }
        if (status.status() == TrialStatusCode.EXPIRED) {
            writeError(response, "TRIAL_INVITE_EXPIRED", "试用期已过期");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : whitelist) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        ErrorResponse body = new ErrorResponse(code, message);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

