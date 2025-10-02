package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.EnterpriseStep1Request;
import com.example.grpcdemo.controller.dto.OnboardingStateResponse;
import com.example.grpcdemo.controller.dto.OnboardingStepRecordDto;
import com.example.grpcdemo.entity.EnterpriseOnboardingSessionEntity;
import com.example.grpcdemo.location.LocationCatalog;
import com.example.grpcdemo.location.LocationCatalog.LocationOption;
import com.example.grpcdemo.onboarding.AnnualHiringPlan;
import com.example.grpcdemo.onboarding.EmployeeScale;
import com.example.grpcdemo.repository.CompanyContactRepository;
import com.example.grpcdemo.repository.CompanyProfileRepository;
import com.example.grpcdemo.repository.EnterpriseOnboardingSessionRepository;
import com.example.grpcdemo.repository.InvitationTemplateRepository;
import com.example.grpcdemo.repository.VerificationTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseOnboardingServiceTest {

    @Mock
    private CompanyProfileRepository companyProfileRepository;

    @Mock
    private CompanyContactRepository companyContactRepository;

    @Mock
    private InvitationTemplateRepository invitationTemplateRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EnterpriseOnboardingSessionRepository sessionRepository;

    @Mock
    private LocationCatalog locationCatalog;

    private ObjectMapper objectMapper;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    void getState_returnsPersistedDrafts_forRehydratingPreviousSteps() {
        when(companyProfileRepository.findByOwnerUserId("user-1")).thenReturn(Optional.empty());

        LocationOption countryOption = new LocationOption("CN", "中国");
        LocationOption cityOption = new LocationOption("CN-11", "北京市");
        when(locationCatalog.findCountry(eq("CN"), any(Locale.class))).thenReturn(Optional.of(countryOption));
        when(locationCatalog.findCity(eq("CN"), eq("CN-11"), any(Locale.class))).thenReturn(Optional.of(cityOption));

        AtomicReference<EnterpriseOnboardingSessionEntity> persisted = new AtomicReference<>();
        when(sessionRepository.save(any(EnterpriseOnboardingSessionEntity.class))).thenAnswer(invocation -> {
            EnterpriseOnboardingSessionEntity entity = invocation.getArgument(0);
            persisted.set(copyEntity(entity));
            return entity;
        });
        when(sessionRepository.findById("user-1")).thenAnswer(invocation -> {
            EnterpriseOnboardingSessionEntity stored = persisted.get();
            if (stored == null) {
                return Optional.empty();
            }
            return Optional.of(copyEntity(stored));
        });

        EnterpriseOnboardingService initialService = new EnterpriseOnboardingService(
                companyProfileRepository,
                companyContactRepository,
                invitationTemplateRepository,
                verificationTokenRepository,
                sessionRepository,
                objectMapper,
                locationCatalog,
                fixedClock);

        EnterpriseStep1Request request = new EnterpriseStep1Request();
        request.setUserId("user-1");
        request.setCompanyName("测试企业");
        request.setCompanyShortName("测试");
        request.setSocialCreditCode("91310000MA1K123X0");
        request.setCountry("CN");
        request.setCity("CN-11");
        request.setEmployeeScale(EmployeeScale.LESS_THAN_FIFTY);
        request.setAnnualHiringPlan(AnnualHiringPlan.ONE_TO_TEN);
        request.setIndustry("互联网");
        request.setWebsite("https://example.com");
        request.setDescription("测试企业简介");

        initialService.saveStep1(request, "zh");

        EnterpriseOnboardingService reloadedService = new EnterpriseOnboardingService(
                companyProfileRepository,
                companyContactRepository,
                invitationTemplateRepository,
                verificationTokenRepository,
                sessionRepository,
                objectMapper,
                locationCatalog,
                fixedClock);

        OnboardingStateResponse state = reloadedService.getState("user-1", "zh");

        assertEquals("user-1", state.getUserId());
        assertEquals(2, state.getCurrentStep());
        assertFalse(state.isCompleted());
        assertNotNull(state.getCompanyInfo());
        assertEquals("测试企业", state.getCompanyInfo().getCompanyName());
        assertEquals("CN", state.getCompanyInfo().getCountry());
        assertEquals("CN-11", state.getCompanyInfo().getCity());
        assertEquals("中国", state.getCompanyInfo().getCountryDisplayName());
        assertEquals("北京市", state.getCompanyInfo().getCityDisplayName());
        assertEquals(EmployeeScale.LESS_THAN_FIFTY, state.getCompanyInfo().getEmployeeScale());
        assertEquals(AnnualHiringPlan.ONE_TO_TEN, state.getCompanyInfo().getAnnualHiringPlan());
        assertNotNull(state.getRecords());
        assertFalse(state.getRecords().isEmpty());
        OnboardingStepRecordDto record = state.getRecords().get(0);
        assertEquals(1, record.getStep());
        assertEquals("测试企业", record.getPayload().get("companyName"));

        EnterpriseStep1Request updated = new EnterpriseStep1Request();
        updated.setUserId("user-1");
        updated.setCompanyName("更新后的企业");
        updated.setCompanyShortName("更新简称");
        updated.setSocialCreditCode("91310000MA1K123X0");
        updated.setCountry("CN");
        updated.setCity("CN-11");
        updated.setEmployeeScale(EmployeeScale.LESS_THAN_FIFTY);
        updated.setAnnualHiringPlan(AnnualHiringPlan.ONE_TO_TEN);
        updated.setIndustry("互联网");
        updated.setWebsite("https://example.com");
        updated.setDescription("更新后的简介");

        reloadedService.saveStep1(updated, "zh");

        EnterpriseOnboardingService afterEditService = new EnterpriseOnboardingService(
                companyProfileRepository,
                companyContactRepository,
                invitationTemplateRepository,
                verificationTokenRepository,
                sessionRepository,
                objectMapper,
                locationCatalog,
                fixedClock);

        OnboardingStateResponse updatedState = afterEditService.getState("user-1", "zh");

        assertEquals("更新后的企业", updatedState.getCompanyInfo().getCompanyName());
        assertEquals("更新后的企业", updatedState.getRecords().get(0).getPayload().get("companyName"));
    }

    private static EnterpriseOnboardingSessionEntity copyEntity(EnterpriseOnboardingSessionEntity source) {
        EnterpriseOnboardingSessionEntity copy = new EnterpriseOnboardingSessionEntity();
        copy.setUserId(source.getUserId());
        copy.setCurrentStep(source.getCurrentStep());
        copy.setStep1Data(source.getStep1Data());
        copy.setStep2Data(source.getStep2Data());
        copy.setStep3Data(source.getStep3Data());
        copy.setRecordsData(source.getRecordsData());
        copy.setExpiresAt(source.getExpiresAt());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }
}
