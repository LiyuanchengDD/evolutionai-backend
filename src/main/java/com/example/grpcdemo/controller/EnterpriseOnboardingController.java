package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.EnterpriseStep1Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep2Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep3Request;
import com.example.grpcdemo.controller.dto.EnterpriseVerifyRequest;
import com.example.grpcdemo.controller.dto.LocationOptionDto;
import com.example.grpcdemo.controller.dto.OnboardingStateResponse;
import com.example.grpcdemo.security.AuthenticatedUser;
import com.example.grpcdemo.service.EnterpriseOnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
    public OnboardingStateResponse getState(@AuthenticationPrincipal AuthenticatedUser user,
                                            @RequestParam("userId") String userId,
                                            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        ensureUserMatches(user, userId);
        return onboardingService.getState(userId, acceptLanguage);

    }

    @GetMapping("/locations/countries")
    public List<LocationOptionDto> listCountries(@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return onboardingService.listCountries(acceptLanguage);
    }

    @GetMapping("/locations/cities")
    public List<LocationOptionDto> listCities(@RequestParam("country") String countryCode,
                                             @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return onboardingService.listCities(countryCode, acceptLanguage);

    }

    @PostMapping("/step1")
    public OnboardingStateResponse saveStep1(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody EnterpriseStep1Request request,
                                            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        ensureUserMatches(user, request.getUserId());
        return onboardingService.saveStep1(request, acceptLanguage);
    }

    @PostMapping("/step2")
    public OnboardingStateResponse saveStep2(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody EnterpriseStep2Request request,
                                            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        ensureUserMatches(user, request.getUserId());
        return onboardingService.saveStep2(request, acceptLanguage);
    }

    @PostMapping("/step3")
    public OnboardingStateResponse saveStep3(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody EnterpriseStep3Request request,
                                            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        ensureUserMatches(user, request.getUserId());
        return onboardingService.saveStep3(request, acceptLanguage);
    }

    @PostMapping("/verify")
    public OnboardingStateResponse verify(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody EnterpriseVerifyRequest request,
                                         @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        ensureUserMatches(user, request.getUserId());
        return onboardingService.verifyAndComplete(request, acceptLanguage);
    }

    private void ensureUserMatches(AuthenticatedUser user, String userId) {
        if (userId == null || !userId.equals(user.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "用户身份不匹配");
        }
    }
}
