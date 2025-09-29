package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.EnterpriseStep1Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep2Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep3Request;
import com.example.grpcdemo.controller.dto.EnterpriseVerifyRequest;
import com.example.grpcdemo.controller.dto.OnboardingStateResponse;
import com.example.grpcdemo.service.EnterpriseOnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing enterprise onboarding step endpoints for the Vue client.
 */
@RestController
@RequestMapping("/api/enterprise/onboarding")
public class EnterpriseOnboardingController {

    private final EnterpriseOnboardingService onboardingService;

    public EnterpriseOnboardingController(EnterpriseOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/state")
    public OnboardingStateResponse getState(@RequestParam("userId") String userId) {
        return onboardingService.getState(userId);
    }

    @PostMapping("/step1")
    public OnboardingStateResponse saveStep1(@Valid @RequestBody EnterpriseStep1Request request) {
        return onboardingService.saveStep1(request);
    }

    @PostMapping("/step2")
    public OnboardingStateResponse saveStep2(@Valid @RequestBody EnterpriseStep2Request request) {
        return onboardingService.saveStep2(request);
    }

    @PostMapping("/step3")
    public OnboardingStateResponse saveStep3(@Valid @RequestBody EnterpriseStep3Request request) {
        return onboardingService.saveStep3(request);
    }

    @PostMapping("/verify")
    public OnboardingStateResponse verify(@Valid @RequestBody EnterpriseVerifyRequest request) {
        return onboardingService.verifyAndComplete(request);
    }
}
