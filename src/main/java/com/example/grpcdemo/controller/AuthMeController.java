package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.AuthMeResponse;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.security.trial.TrialService;
import com.example.grpcdemo.security.trial.TrialStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthMeController {

    private final TrialService trialService;

    public AuthMeController(TrialService trialService) {
        this.trialService = trialService;
    }

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal AuthenticatedUser user,
                             HttpServletRequest request) {
        TrialStatus status = (TrialStatus) request.getAttribute(TrialStatus.REQUEST_ATTRIBUTE);
        if (status == null) {
            status = trialService.evaluate(user.email());
        }
        AuthMeResponse.Trial trialDto = new AuthMeResponse.Trial(
                status.status().name().toLowerCase(),
                status.expiresAt()
        );
        return new AuthMeResponse(user.userId(), user.email(), user.devToken(), trialDto);
    }
}

