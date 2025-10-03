package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.CallingCodeDto;
import com.example.grpcdemo.controller.dto.CompanyInfoUpdateRequest;
import com.example.grpcdemo.controller.dto.CompanyProfileResponse;
import com.example.grpcdemo.controller.dto.CreateHrRequest;
import com.example.grpcdemo.controller.dto.HrContactResponse;
import com.example.grpcdemo.controller.dto.HrContactUpdateRequest;
import com.example.grpcdemo.controller.dto.PasswordSuggestionResponse;
import com.example.grpcdemo.service.CompanyProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints used by the enterprise portal to manage company and HR
 * information after onboarding.
 */
@RestController
@RequestMapping("/api/enterprise/profile")
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    @GetMapping
    public CompanyProfileResponse getProfile(@RequestParam("userId") String userId,
                                             @RequestHeader(value = "Accept-Language", required = false) String language) {
        return companyProfileService.getProfile(userId, language);
    }

    @PutMapping("/company")
    public CompanyProfileResponse updateCompany(@Valid @RequestBody CompanyInfoUpdateRequest request,
                                                @RequestHeader(value = "Accept-Language", required = false) String language) {
        return companyProfileService.updateCompanyInfo(request, language);
    }

    @PostMapping("/hr")
    public HrContactResponse createContact(@Valid @RequestBody CreateHrRequest request) {
        return companyProfileService.createContact(request);
    }

    @PutMapping("/hr/{contactId}")
    public HrContactResponse updateContact(@PathVariable("contactId") String contactId,
                                           @Valid @RequestBody HrContactUpdateRequest request) {
        return companyProfileService.updateContact(contactId, request);
    }

    @GetMapping("/calling-codes")
    public List<CallingCodeDto> listCallingCodes(@RequestHeader(value = "Accept-Language", required = false) String language) {
        return companyProfileService.listCallingCodes(language);
    }

    @GetMapping("/password/suggestion")
    public PasswordSuggestionResponse suggestPassword() {
        return companyProfileService.generatePassword();
    }
}
