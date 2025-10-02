package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.EnterpriseCompanyInfoDto;
import com.example.grpcdemo.controller.dto.EnterpriseContactInfoDto;
import com.example.grpcdemo.controller.dto.EnterpriseStep1Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep2Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep3Request;
import com.example.grpcdemo.controller.dto.EnterpriseTemplateDto;
import com.example.grpcdemo.controller.dto.EnterpriseVerifyRequest;
import com.example.grpcdemo.controller.dto.OnboardingStateResponse;
import com.example.grpcdemo.controller.dto.OnboardingStepRecordDto;
import com.example.grpcdemo.entity.CompanyContactEntity;
import com.example.grpcdemo.entity.CompanyProfileEntity;
import com.example.grpcdemo.entity.EnterpriseOnboardingSessionEntity;
import com.example.grpcdemo.entity.InvitationTemplateEntity;
import com.example.grpcdemo.entity.VerificationTokenEntity;
import com.example.grpcdemo.onboarding.CompanyStatus;
import com.example.grpcdemo.onboarding.EmployeeScale;
import com.example.grpcdemo.onboarding.EnterpriseVerificationPurpose;
import com.example.grpcdemo.onboarding.OnboardingErrorCode;
import com.example.grpcdemo.onboarding.OnboardingException;
import com.example.grpcdemo.repository.CompanyContactRepository;
import com.example.grpcdemo.repository.CompanyProfileRepository;
import com.example.grpcdemo.repository.InvitationTemplateRepository;
import com.example.grpcdemo.repository.EnterpriseOnboardingSessionRepository;
import com.example.grpcdemo.repository.VerificationTokenRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Coordinates the in-memory onboarding flow until the enterprise verifies its email
 * and the data can be persisted.
 */
@Service
public class EnterpriseOnboardingService {

    private static final List<String> AVAILABLE_TEMPLATE_VARIABLES = List.of(
            "[[候选人姓名]]",
            "[[岗位名称]]",
            "[[企业全称]]",
            "[[面试时间]]",
            "[[面试地点]]",
            "[[面试链接]]",
            "[[联系人姓名]]",
            "[[联系人电话]]",
            "[[联系人邮箱]]"
    );

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[\\[(.+?)]]");

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final Map<String, EnterpriseOnboardingSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyContactRepository companyContactRepository;
    private final InvitationTemplateRepository invitationTemplateRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EnterpriseOnboardingSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EnterpriseOnboardingService(CompanyProfileRepository companyProfileRepository,
                                       CompanyContactRepository companyContactRepository,
                                       InvitationTemplateRepository invitationTemplateRepository,
                                       VerificationTokenRepository verificationTokenRepository,
                                       EnterpriseOnboardingSessionRepository sessionRepository,
                                       ObjectMapper objectMapper) {
        this(companyProfileRepository,
                companyContactRepository,
                invitationTemplateRepository,
                verificationTokenRepository,
                sessionRepository,
                objectMapper,
                Clock.systemUTC());
    }

    EnterpriseOnboardingService(CompanyProfileRepository companyProfileRepository,
                                CompanyContactRepository companyContactRepository,
                                InvitationTemplateRepository invitationTemplateRepository,
                                VerificationTokenRepository verificationTokenRepository,
                                EnterpriseOnboardingSessionRepository sessionRepository,
                                ObjectMapper objectMapper,
                                Clock clock) {
        this.companyProfileRepository = companyProfileRepository;
        this.companyContactRepository = companyContactRepository;
        this.invitationTemplateRepository = invitationTemplateRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public OnboardingStateResponse saveStep1(EnterpriseStep1Request request) {
        ensureNotCompleted(request.getUserId());
        Object lock = sessionLocks.computeIfAbsent(request.getUserId(), key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = loadSession(request.getUserId(), true);
            Step1Data data = new Step1Data(
                    request.getCompanyName(),
                    request.getCompanyShortName(),
                    request.getSocialCreditCode(),
                    request.getCountry(),
                    request.getCity(),
                    request.getEmployeeScale(),
                    request.getIndustry(),
                    request.getWebsite(),
                    request.getDescription()
            );
            Instant now = clock.instant();
            session.setStep1(data, now);
            session.setCurrentStep(2);
            session.refreshExpiration(now.plus(SESSION_TTL));
            persistSession(session, now);
            return buildStateFromSession(session);
        }
    }

    public OnboardingStateResponse saveStep2(EnterpriseStep2Request request) {
        ensureNotCompleted(request.getUserId());
        Object lock = sessionLocks.computeIfAbsent(request.getUserId(), key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = requireSession(request.getUserId());
            if (session.getStep1() == null) {
                throw new OnboardingException(OnboardingErrorCode.MISSING_PREVIOUS_STEP);
            }
            Step2Data data = new Step2Data(
                    request.getContactName(),
                    request.getContactEmail(),
                    request.getPhoneCountryCode(),
                    request.getPhoneNumber(),
                    request.getPosition(),
                    request.getDepartment()
            );
            Instant now = clock.instant();
            session.setStep2(data, now);
            session.setCurrentStep(3);
            session.refreshExpiration(now.plus(SESSION_TTL));
            persistSession(session, now);
            return buildStateFromSession(session);
        }
    }

    public OnboardingStateResponse saveStep3(EnterpriseStep3Request request) {
        ensureNotCompleted(request.getUserId());
        Object lock = sessionLocks.computeIfAbsent(request.getUserId(), key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = requireSession(request.getUserId());
            if (session.getStep2() == null) {
                throw new OnboardingException(OnboardingErrorCode.MISSING_PREVIOUS_STEP);
            }
            List<String> variables = extractVariables(request.getSubject(), request.getBody());
            Step3Data data = new Step3Data(
                    request.getTemplateName(),
                    request.getSubject(),
                    request.getBody(),
                    StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "zh-CN",
                    variables
            );
            Instant now = clock.instant();
            session.setStep3(data, now);
            session.setCurrentStep(4);
            session.refreshExpiration(now.plus(SESSION_TTL));
            persistSession(session, now);
            return buildStateFromSession(session);
        }
    }

    @Transactional
    public OnboardingStateResponse verifyAndComplete(EnterpriseVerifyRequest request) {
        ensureNotCompleted(request.getUserId());
        Object lock = sessionLocks.computeIfAbsent(request.getUserId(), key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = requireSession(request.getUserId());
            if (session.getStep3() == null) {
                throw new OnboardingException(OnboardingErrorCode.MISSING_PREVIOUS_STEP);
            }
            Step1Data step1 = session.getStep1();
            Step2Data step2 = session.getStep2();
            Step3Data step3 = session.getStep3();
            Instant now = clock.instant();

            VerificationTokenEntity token = verificationTokenRepository
                    .findTopByTargetUserIdAndPurposeAndCodeAndConsumedFalseOrderByCreatedAtDesc(
                            request.getUserId(),
                            EnterpriseVerificationPurpose.ENTERPRISE_ONBOARDING,
                            request.getVerificationCode())
                    .orElseThrow(() -> new OnboardingException(OnboardingErrorCode.INVALID_VERIFICATION_CODE));
            if (token.getExpiresAt().isBefore(now)) {
                throw new OnboardingException(OnboardingErrorCode.VERIFICATION_CODE_EXPIRED);
            }
            if (StringUtils.hasText(request.getEmail()) && !Objects.equals(token.getTargetEmail(), request.getEmail())) {
                throw new OnboardingException(OnboardingErrorCode.INVALID_VERIFICATION_CODE, "验证码与邮箱不匹配");
            }

            token.setConsumed(true);
            token.setUpdatedAt(now);
            verificationTokenRepository.save(token);

            String companyId = UUID.randomUUID().toString();
            CompanyProfileEntity profile = createCompanyProfileEntity(step1, request.getUserId(), companyId, now);
            CompanyContactEntity contact = createContactEntity(step2, companyId, now);
            InvitationTemplateEntity template = createTemplateEntity(step3, companyId, now);

            companyProfileRepository.save(profile);
            companyContactRepository.save(contact);
            invitationTemplateRepository.save(template);

            List<OnboardingStepRecordDto> records = session.toRecordDtos();
            sessions.remove(request.getUserId());
            if (sessionRepository.existsById(request.getUserId())) {
                sessionRepository.deleteById(request.getUserId());
            }
            sessionLocks.remove(request.getUserId());

            return buildCompletedState(request.getUserId(), companyId, step1, step2, step3, records);
        }
    }

    public OnboardingStateResponse getState(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new OnboardingException(OnboardingErrorCode.SESSION_NOT_FOUND);
        }
        Optional<CompanyProfileEntity> existing = companyProfileRepository.findByOwnerUserId(userId);
        if (existing.isPresent()) {
            CompanyProfileEntity profile = existing.get();
            CompanyContactEntity contact = companyContactRepository
                    .findFirstByCompanyIdOrderByCreatedAtAsc(profile.getCompanyId())
                    .orElse(null);
            InvitationTemplateEntity template = invitationTemplateRepository
                    .findFirstByCompanyIdAndDefaultTemplateTrue(profile.getCompanyId())
                    .orElse(null);
            return buildCompletedStateFromPersistence(userId, profile, contact, template);
        }
        Object lock = sessionLocks.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = loadSession(userId, false);
            if (session == null) {
                OnboardingStateResponse response = new OnboardingStateResponse();
                response.setUserId(userId);
                response.setCurrentStep(1);
                response.setCompleted(false);
                response.setAvailableVariables(AVAILABLE_TEMPLATE_VARIABLES);
                response.setRecords(Collections.emptyList());
                return response;
            }
            return buildStateFromSession(session);
        }
    }

    private void ensureNotCompleted(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new OnboardingException(OnboardingErrorCode.SESSION_NOT_FOUND);
        }
        companyProfileRepository.findByOwnerUserId(userId).ifPresent(profile -> {
            throw new OnboardingException(OnboardingErrorCode.ONBOARDING_ALREADY_COMPLETED);
        });
    }

    private EnterpriseOnboardingSession requireSession(String userId) {
        EnterpriseOnboardingSession session = loadSession(userId, false);
        if (session == null) {
            throw new OnboardingException(OnboardingErrorCode.SESSION_NOT_FOUND);
        }
        return session;
    }

    private OnboardingStateResponse buildStateFromSession(EnterpriseOnboardingSession session) {
        OnboardingStateResponse response = new OnboardingStateResponse();
        response.setUserId(session.getUserId());
        response.setCurrentStep(session.getCurrentStep());
        response.setCompleted(false);
        response.setCompanyInfo(session.getStep1() != null ? toCompanyDto(session.getStep1()) : null);
        response.setContactInfo(session.getStep2() != null ? toContactDto(session.getStep2()) : null);
        response.setTemplateInfo(session.getStep3() != null ? toTemplateDto(session.getStep3()) : null);
        response.setRecords(session.toRecordDtos());
        response.setAvailableVariables(AVAILABLE_TEMPLATE_VARIABLES);
        return response;
    }

    private OnboardingStateResponse buildCompletedState(String userId,
                                                        String companyId,
                                                        Step1Data step1,
                                                        Step2Data step2,
                                                        Step3Data step3,
                                                        List<OnboardingStepRecordDto> records) {
        OnboardingStateResponse response = new OnboardingStateResponse();
        response.setUserId(userId);
        response.setCompanyId(companyId);
        response.setCurrentStep(4);
        response.setCompleted(true);
        response.setCompanyInfo(toCompanyDto(step1));
        response.setContactInfo(toContactDto(step2));
        response.setTemplateInfo(toTemplateDto(step3));
        response.setRecords(records);
        response.setAvailableVariables(AVAILABLE_TEMPLATE_VARIABLES);
        return response;
    }

    private OnboardingStateResponse buildCompletedStateFromPersistence(String userId,
                                                                       CompanyProfileEntity profile,
                                                                       CompanyContactEntity contact,
                                                                       InvitationTemplateEntity template) {
        OnboardingStateResponse response = new OnboardingStateResponse();
        response.setUserId(userId);
        response.setCompanyId(profile.getCompanyId());
        response.setCurrentStep(4);
        response.setCompleted(true);
        response.setCompanyInfo(toCompanyDto(profile));
        if (contact != null) {
            response.setContactInfo(toContactDto(contact));
        }
        if (template != null) {
            response.setTemplateInfo(toTemplateDto(template));
        }
        response.setRecords(Collections.emptyList());
        response.setAvailableVariables(AVAILABLE_TEMPLATE_VARIABLES);
        return response;
    }

    private EnterpriseOnboardingSession loadSession(String userId, boolean createIfMissing) {
        Instant now = clock.instant();
        EnterpriseOnboardingSession session = sessions.get(userId);
        if (session != null && session.isExpired(now)) {
            sessions.remove(userId);
            session = null;
        }
        if (session == null) {
            sessionRepository.deleteByExpiresAtBefore(now);
            session = sessionRepository.findById(userId)
                    .map(this::fromEntity)
                    .orElse(null);
            if (session != null) {
                if (session.isExpired(now)) {
                    sessionRepository.deleteById(userId);
                    session = null;
                } else {
                    sessions.put(userId, session);
                }
            }
        }
        if (session == null && createIfMissing) {
            session = new EnterpriseOnboardingSession(userId);
            session.refreshExpiration(now.plus(SESSION_TTL));
            sessions.put(userId, session);
        }
        return session;
    }

    private EnterpriseOnboardingSession fromEntity(EnterpriseOnboardingSessionEntity entity) {
        EnterpriseOnboardingSession session = new EnterpriseOnboardingSession(entity.getUserId());
        session.currentStep = entity.getCurrentStep();
        session.step1 = readJson(entity.getStep1Data(), Step1Data.class);
        session.step2 = readJson(entity.getStep2Data(), Step2Data.class);
        session.step3 = readJson(entity.getStep3Data(), Step3Data.class);
        session.replaceRecords(readRecordStates(entity.getRecordsData()));
        session.setExpiresAt(entity.getExpiresAt());
        return session;
    }

    private void persistSession(EnterpriseOnboardingSession session, Instant now) {
        EnterpriseOnboardingSessionEntity entity = sessionRepository.findById(session.getUserId())
                .orElseGet(() -> {
                    EnterpriseOnboardingSessionEntity created = new EnterpriseOnboardingSessionEntity();
                    created.setUserId(session.getUserId());
                    created.setCreatedAt(now);
                    return created;
                });
        entity.setCurrentStep(session.getCurrentStep());
        entity.setStep1Data(writeJson(session.getStep1()));
        entity.setStep2Data(writeJson(session.getStep2()));
        entity.setStep3Data(writeJson(session.getStep3()));
        entity.setRecordsData(writeJson(session.toRecordStates()));
        entity.setExpiresAt(session.getExpiresAt());
        entity.setUpdatedAt(now);
        sessionRepository.save(entity);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize onboarding session state", e);
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize onboarding session state", e);
        }
    }

    private List<StepRecordState> readRecordStates(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<StepRecordState>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize onboarding step records", e);
        }
    }

    private CompanyProfileEntity createCompanyProfileEntity(Step1Data data, String userId, String companyId, Instant now) {
        CompanyProfileEntity profile = new CompanyProfileEntity();
        profile.setCompanyId(companyId);
        profile.setOwnerUserId(userId);
        profile.setCompanyName(data.companyName);
        profile.setCompanyShortName(data.companyShortName);
        profile.setSocialCreditCode(data.socialCreditCode);
        profile.setCountry(data.country);
        profile.setCity(data.city);
        profile.setEmployeeScale(data.employeeScale);
        profile.setIndustry(data.industry);
        profile.setWebsite(data.website);
        profile.setDescription(data.description);
        profile.setStatus(CompanyStatus.ACTIVE);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return profile;
    }

    private CompanyContactEntity createContactEntity(Step2Data data, String companyId, Instant now) {
        CompanyContactEntity contact = new CompanyContactEntity();
        contact.setContactId(UUID.randomUUID().toString());
        contact.setCompanyId(companyId);
        contact.setContactName(data.contactName);
        contact.setContactEmail(data.contactEmail);
        contact.setPhoneCountryCode(data.phoneCountryCode);
        contact.setPhoneNumber(data.phoneNumber);
        contact.setPosition(data.position);
        contact.setDepartment(data.department);
        contact.setPrimaryContact(true);
        contact.setCreatedAt(now);
        contact.setUpdatedAt(now);
        return contact;
    }

    private InvitationTemplateEntity createTemplateEntity(Step3Data data, String companyId, Instant now) {
        InvitationTemplateEntity template = new InvitationTemplateEntity();
        template.setTemplateId(UUID.randomUUID().toString());
        template.setCompanyId(companyId);
        template.setTemplateName(data.templateName);
        template.setSubject(data.subject);
        template.setBody(data.body);
        template.setLanguage(data.language);
        template.setDefaultTemplate(true);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        return template;
    }

    private EnterpriseCompanyInfoDto toCompanyDto(Step1Data data) {
        EnterpriseCompanyInfoDto dto = new EnterpriseCompanyInfoDto();
        dto.setCompanyName(data.companyName);
        dto.setCompanyShortName(data.companyShortName);
        dto.setSocialCreditCode(data.socialCreditCode);
        dto.setCountry(data.country);
        dto.setCity(data.city);
        dto.setEmployeeScale(data.employeeScale);
        dto.setIndustry(data.industry);
        dto.setWebsite(data.website);
        dto.setDescription(data.description);
        return dto;
    }

    private EnterpriseCompanyInfoDto toCompanyDto(CompanyProfileEntity entity) {
        EnterpriseCompanyInfoDto dto = new EnterpriseCompanyInfoDto();
        dto.setCompanyName(entity.getCompanyName());
        dto.setCompanyShortName(entity.getCompanyShortName());
        dto.setSocialCreditCode(entity.getSocialCreditCode());
        dto.setCountry(entity.getCountry());
        dto.setCity(entity.getCity());
        dto.setEmployeeScale(entity.getEmployeeScale());
        dto.setIndustry(entity.getIndustry());
        dto.setWebsite(entity.getWebsite());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private EnterpriseContactInfoDto toContactDto(Step2Data data) {
        EnterpriseContactInfoDto dto = new EnterpriseContactInfoDto();
        dto.setContactName(data.contactName);
        dto.setContactEmail(data.contactEmail);
        dto.setPhoneCountryCode(data.phoneCountryCode);
        dto.setPhoneNumber(data.phoneNumber);
        dto.setPosition(data.position);
        dto.setDepartment(data.department);
        return dto;
    }

    private EnterpriseContactInfoDto toContactDto(CompanyContactEntity entity) {
        EnterpriseContactInfoDto dto = new EnterpriseContactInfoDto();
        dto.setContactName(entity.getContactName());
        dto.setContactEmail(entity.getContactEmail());
        dto.setPhoneCountryCode(entity.getPhoneCountryCode());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setPosition(entity.getPosition());
        dto.setDepartment(entity.getDepartment());
        return dto;
    }

    private EnterpriseTemplateDto toTemplateDto(Step3Data data) {
        EnterpriseTemplateDto dto = new EnterpriseTemplateDto();
        dto.setTemplateName(data.templateName);
        dto.setSubject(data.subject);
        dto.setBody(data.body);
        dto.setLanguage(data.language);
        dto.setVariables(data.variables);
        return dto;
    }

    private EnterpriseTemplateDto toTemplateDto(InvitationTemplateEntity entity) {
        EnterpriseTemplateDto dto = new EnterpriseTemplateDto();
        dto.setTemplateName(entity.getTemplateName());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setLanguage(entity.getLanguage());
        dto.setVariables(extractVariables(entity.getSubject(), entity.getBody()));
        return dto;
    }

    private List<String> extractVariables(String subject, String body) {
        Set<String> variables = new LinkedHashSet<>();
        Matcher subjectMatcher = PLACEHOLDER_PATTERN.matcher(subject != null ? subject : "");
        while (subjectMatcher.find()) {
            String variable = "[[" + subjectMatcher.group(1) + "]]";
            variables.add(variable);
        }
        Matcher bodyMatcher = PLACEHOLDER_PATTERN.matcher(body != null ? body : "");
        while (bodyMatcher.find()) {
            String variable = "[[" + bodyMatcher.group(1) + "]]";
            variables.add(variable);
        }
        for (String variable : variables) {
            if (!AVAILABLE_TEMPLATE_VARIABLES.contains(variable)) {
                throw new OnboardingException(OnboardingErrorCode.INVALID_TEMPLATE_VARIABLE,
                        "变量 " + variable + " 未在允许列表中");
            }
        }
        return new ArrayList<>(variables);
    }

    private static class EnterpriseOnboardingSession {
        private final String userId;
        private final List<StepRecord> records = new ArrayList<>();
        private Step1Data step1;
        private Step2Data step2;
        private Step3Data step3;
        private int currentStep = 1;
        private Instant expiresAt = Instant.EPOCH;

        private EnterpriseOnboardingSession(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public Step1Data getStep1() {
            return step1;
        }

        public void setStep1(Step1Data step1, Instant savedAt) {
            this.step1 = step1;
            saveRecord(1, step1.toMap(), savedAt);
        }

        public Step2Data getStep2() {
            return step2;
        }

        public void setStep2(Step2Data step2, Instant savedAt) {
            this.step2 = step2;
            saveRecord(2, step2.toMap(), savedAt);
        }

        public Step3Data getStep3() {
            return step3;
        }

        public void setStep3(Step3Data step3, Instant savedAt) {
            this.step3 = step3;
            saveRecord(3, step3.toMap(), savedAt);
        }

        public int getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(int currentStep) {
            this.currentStep = currentStep;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        public void refreshExpiration(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        public boolean isExpired(Instant now) {
            return expiresAt != null && now.isAfter(expiresAt);
        }

        public List<OnboardingStepRecordDto> toRecordDtos() {
            return records.stream()
                    .map(record -> new OnboardingStepRecordDto(record.step, record.savedAt, record.payload))
                    .collect(Collectors.toList());
        }

        public List<StepRecordState> toRecordStates() {
            return records.stream()
                    .map(record -> new StepRecordState(record.step, record.savedAt, record.payload))
                    .collect(Collectors.toList());
        }

        public void replaceRecords(List<StepRecordState> states) {
            records.clear();
            if (states == null) {
                return;
            }
            for (StepRecordState state : states) {
                if (state == null) {
                    continue;
                }
                records.add(new StepRecord(state.step(), state.savedAt(), state.payload()));
            }
            records.sort((a, b) -> Integer.compare(a.step, b.step));
        }

        private void saveRecord(int step, Map<String, Object> payload, Instant savedAt) {
            StepRecord existing = records.stream()
                    .filter(record -> record.step == step)
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                existing.payload = payload;
                existing.savedAt = savedAt;
            } else {
                records.add(new StepRecord(step, savedAt, payload));
                records.sort((a, b) -> Integer.compare(a.step, b.step));
            }
        }
    }

    private record StepRecordState(int step, Instant savedAt, Map<String, Object> payload) {
    }

    private record Step1Data(String companyName,
                             String companyShortName,
                             String socialCreditCode,
                             String country,
                             String city,
                             EmployeeScale employeeScale,
                             String industry,
                             String website,
                             String description) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("companyName", companyName);
            map.put("companyShortName", companyShortName);
            map.put("socialCreditCode", socialCreditCode);
            map.put("country", country);
            map.put("city", city);
            map.put("employeeScale", employeeScale != null ? employeeScale.name() : null);
            map.put("industry", industry);
            map.put("website", website);
            map.put("description", description);
            return map;
        }
    }

    private record Step2Data(String contactName,
                             String contactEmail,
                             String phoneCountryCode,
                             String phoneNumber,
                             String position,
                             String department) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("contactName", contactName);
            map.put("contactEmail", contactEmail);
            map.put("phoneCountryCode", phoneCountryCode);
            map.put("phoneNumber", phoneNumber);
            map.put("position", position);
            map.put("department", department);
            return map;
        }
    }

    private record Step3Data(String templateName,
                             String subject,
                             String body,
                             String language,
                             List<String> variables) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("templateName", templateName);
            map.put("subject", subject);
            map.put("body", body);
            map.put("language", language);
            map.put("variables", variables);
            return map;
        }
    }

    private static class StepRecord {
        private final int step;
        private Instant savedAt;
        private Map<String, Object> payload;

        private StepRecord(int step, Instant savedAt, Map<String, Object> payload) {
            this.step = step;
            this.savedAt = savedAt;
            this.payload = payload;
        }
    }
}
