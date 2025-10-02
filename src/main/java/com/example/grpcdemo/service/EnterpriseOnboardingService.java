package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.EnterpriseCompanyInfoDto;
import com.example.grpcdemo.controller.dto.EnterpriseContactInfoDto;
import com.example.grpcdemo.controller.dto.EnterpriseStep1Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep2Request;
import com.example.grpcdemo.controller.dto.EnterpriseStep3Request;
import com.example.grpcdemo.controller.dto.EnterpriseTemplateDto;
import com.example.grpcdemo.controller.dto.EnterpriseVerifyRequest;
import com.example.grpcdemo.controller.dto.LocationOptionDto;
import com.example.grpcdemo.controller.dto.OnboardingStateResponse;
import com.example.grpcdemo.controller.dto.OnboardingStepRecordDto;
import com.example.grpcdemo.entity.CompanyContactEntity;
import com.example.grpcdemo.entity.CompanyProfileEntity;
import com.example.grpcdemo.entity.CompanyRecruitingPositionEntity;
import com.example.grpcdemo.entity.EnterpriseOnboardingSessionEntity;
import com.example.grpcdemo.entity.InvitationTemplateEntity;
import com.example.grpcdemo.entity.VerificationTokenEntity;
import com.example.grpcdemo.location.LocationCatalog;
import com.example.grpcdemo.location.LocationCatalog.LocationOption;
import com.example.grpcdemo.onboarding.AnnualHiringPlan;
import com.example.grpcdemo.onboarding.CompanyStatus;
import com.example.grpcdemo.onboarding.EmployeeScale;
import com.example.grpcdemo.onboarding.EnterpriseVerificationPurpose;
import com.example.grpcdemo.onboarding.OnboardingErrorCode;
import com.example.grpcdemo.onboarding.OnboardingException;
import com.example.grpcdemo.repository.CompanyContactRepository;
import com.example.grpcdemo.repository.CompanyProfileRepository;
import com.example.grpcdemo.repository.CompanyRecruitingPositionRepository;
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
import java.util.Locale;
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

    private static final String DEFAULT_TEMPLATE_LANGUAGE = "zh";

    private static final Map<String, List<String>> AVAILABLE_TEMPLATE_VARIABLES_BY_LANGUAGE;

    static {
        Map<String, List<String>> variables = new LinkedHashMap<>();
        variables.put("zh", List.of(
                "[[候选人姓名]]",
                "[[岗位名称]]",
                "[[企业全称]]",
                "[[面试时间]]",
                "[[面试地点]]",
                "[[面试链接]]",
                "[[联系人姓名]]",
                "[[联系人电话]]",
                "[[联系人邮箱]]"
        ));
        variables.put("en", List.of(
                "[[Candidate Name]]",
                "[[Job Title]]",
                "[[Company Name]]",
                "[[Interview Time]]",
                "[[Interview Location]]",
                "[[Interview Link]]",
                "[[Contact Name]]",
                "[[Contact Phone]]",
                "[[Contact Email]]"
        ));
        variables.put("jp", List.of(
                "[[候補者名]]",
                "[[職位名]]",
                "[[企業名]]",
                "[[面接時間]]",
                "[[面接場所]]",
                "[[面接リンク]]",
                "[[担当者名]]",
                "[[担当者電話]]",
                "[[担当者メール]]"
        ));
        AVAILABLE_TEMPLATE_VARIABLES_BY_LANGUAGE = Collections.unmodifiableMap(variables);
    }

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[\\[(.+?)]]");

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final Map<String, EnterpriseOnboardingSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyContactRepository companyContactRepository;
    private final InvitationTemplateRepository invitationTemplateRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EnterpriseOnboardingSessionRepository sessionRepository;
    private final CompanyRecruitingPositionRepository recruitingPositionRepository;
    private final LocationCatalog locationCatalog;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EnterpriseOnboardingService(CompanyProfileRepository companyProfileRepository,
                                       CompanyContactRepository companyContactRepository,
                                       InvitationTemplateRepository invitationTemplateRepository,
                                       VerificationTokenRepository verificationTokenRepository,
                                       EnterpriseOnboardingSessionRepository sessionRepository,
                                       CompanyRecruitingPositionRepository recruitingPositionRepository,
                                       ObjectMapper objectMapper,
                                       LocationCatalog locationCatalog) {
        this(companyProfileRepository,
                companyContactRepository,
                invitationTemplateRepository,
                verificationTokenRepository,
                sessionRepository,
                recruitingPositionRepository,
                objectMapper,
                locationCatalog,
                Clock.systemUTC());
    }

    EnterpriseOnboardingService(CompanyProfileRepository companyProfileRepository,
                                CompanyContactRepository companyContactRepository,
                                InvitationTemplateRepository invitationTemplateRepository,
                                VerificationTokenRepository verificationTokenRepository,
                                EnterpriseOnboardingSessionRepository sessionRepository,
                                CompanyRecruitingPositionRepository recruitingPositionRepository,
                                ObjectMapper objectMapper,
                                LocationCatalog locationCatalog,
                                Clock clock) {
        this.companyProfileRepository = companyProfileRepository;
        this.companyContactRepository = companyContactRepository;
        this.invitationTemplateRepository = invitationTemplateRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.sessionRepository = sessionRepository;
        this.recruitingPositionRepository = recruitingPositionRepository;
        this.locationCatalog = locationCatalog;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public OnboardingStateResponse saveStep1(EnterpriseStep1Request request, String preferredLanguage) {
        ensureNotCompleted(request.getUserId());
        Locale locale = resolveLocale(preferredLanguage);
        Object lock = sessionLocks.computeIfAbsent(request.getUserId(), key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = loadSession(request.getUserId(), true);
            String countryCode = normalizeCountryCode(request.getCountry());
            String cityCode = normalizeCityCode(request.getCity());
            if (!StringUtils.hasText(countryCode)) {
                throw new OnboardingException(OnboardingErrorCode.INVALID_LOCATION, "不支持的国家编码");
            }
            if (!StringUtils.hasText(cityCode)) {
                throw new OnboardingException(OnboardingErrorCode.INVALID_LOCATION, "不支持的城市编码");
            }
            LocationOption country = locationCatalog.findCountry(countryCode, locale)
                    .orElseThrow(() -> new OnboardingException(OnboardingErrorCode.INVALID_LOCATION, "不支持的国家编码"));
            LocationOption city = locationCatalog.findCity(countryCode, cityCode, locale)
                    .orElseThrow(() -> new OnboardingException(OnboardingErrorCode.INVALID_LOCATION, "不支持的城市编码"));
            LocationOption persistedCountry = locationCatalog.findCountry(countryCode, Locale.SIMPLIFIED_CHINESE)
                    .orElse(country);
            LocationOption persistedCity = locationCatalog.findCity(countryCode, cityCode, Locale.SIMPLIFIED_CHINESE)
                    .orElse(city);
            Step1Data data = new Step1Data(
                    request.getCompanyName(),
                    request.getCompanyShortName(),
                    request.getSocialCreditCode(),
                    country.code(),
                    persistedCountry.name(),
                    city.code(),
                    persistedCity.name(),
                    request.getEmployeeScale(),
                    request.getAnnualHiringPlan(),
                    request.getIndustry(),
                    request.getWebsite(),
                    request.getDescription(),
                    request.getDetailedAddress(),
                    sanitizeRecruitingPositions(request.getRecruitingPositions())
            );
            Instant now = clock.instant();
            session.setStep1(data, now);
            session.setCurrentStep(2);
            session.refreshExpiration(now.plus(SESSION_TTL));
            persistSession(session, now);
            return buildStateFromSession(session, preferredLanguage);
        }
    }

    public OnboardingStateResponse saveStep2(EnterpriseStep2Request request, String preferredLanguage) {
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
            return buildStateFromSession(session, preferredLanguage);
        }
    }

    public OnboardingStateResponse saveStep3(EnterpriseStep3Request request, String preferredLanguage) {
        ensureNotCompleted(request.getUserId());
        Object lock = sessionLocks.computeIfAbsent(request.getUserId(), key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = requireSession(request.getUserId());
            if (session.getStep2() == null) {
                throw new OnboardingException(OnboardingErrorCode.MISSING_PREVIOUS_STEP);
            }
            String language = determineLanguage(request.getLanguage(), preferredLanguage);
            List<String> variables = extractVariables(request.getSubject(), request.getBody(), language);
            Step3Data data = new Step3Data(
                    request.getTemplateName(),
                    request.getSubject(),
                    request.getBody(),
                    language,
                    variables
            );
            Instant now = clock.instant();
            session.setStep3(data, now);
            session.setCurrentStep(4);
            session.refreshExpiration(now.plus(SESSION_TTL));
            persistSession(session, now);
            return buildStateFromSession(session, preferredLanguage);
        }
    }

    @Transactional
    public OnboardingStateResponse verifyAndComplete(EnterpriseVerifyRequest request, String preferredLanguage) {
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
            List<CompanyRecruitingPositionEntity> recruitingPositions =
                    createRecruitingPositionEntities(step1.recruitingPositions, companyId, now);

            companyProfileRepository.save(profile);
            companyContactRepository.save(contact);
            invitationTemplateRepository.save(template);
            if (!recruitingPositions.isEmpty()) {
                recruitingPositionRepository.saveAll(recruitingPositions);
            }

            List<OnboardingStepRecordDto> records = session.toRecordDtos();
            sessions.remove(request.getUserId());
            if (sessionRepository.existsById(request.getUserId())) {
                sessionRepository.deleteById(request.getUserId());
            }
            sessionLocks.remove(request.getUserId());

            return buildCompletedState(request.getUserId(),
                    companyId,
                    step1,
                    step2,
                    step3,
                    records,
                    preferredLanguage);
        }
    }

    public List<LocationOptionDto> listCountries(String preferredLanguage) {
        Locale locale = resolveLocale(preferredLanguage);
        return locationCatalog.listCountries(locale).stream()
                .map(option -> new LocationOptionDto(option.code(), option.name()))
                .collect(Collectors.toList());
    }

    public List<LocationOptionDto> listCities(String countryCode, String preferredLanguage) {
        Locale locale = resolveLocale(preferredLanguage);
        String normalizedCountry = normalizeCountryCode(countryCode);
        return locationCatalog.listCities(normalizedCountry, locale).stream()
                .map(option -> new LocationOptionDto(option.code(), option.name()))
                .collect(Collectors.toList());
    }

    public OnboardingStateResponse getState(String userId, String preferredLanguage) {
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
            return buildCompletedStateFromPersistence(userId, profile, contact, template, preferredLanguage);
        }
        Object lock = sessionLocks.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            EnterpriseOnboardingSession session = loadSession(userId, false);
            if (session == null) {
                OnboardingStateResponse response = new OnboardingStateResponse();
                response.setUserId(userId);
                response.setCurrentStep(1);
                response.setCompleted(false);
                populateAvailableVariables(response, preferredLanguage, null);
                response.setRecords(Collections.emptyList());
                return response;
            }
            return buildStateFromSession(session, preferredLanguage);
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

    private OnboardingStateResponse buildStateFromSession(EnterpriseOnboardingSession session, String preferredLanguage) {
        Locale locale = resolveLocale(preferredLanguage);

        OnboardingStateResponse response = new OnboardingStateResponse();
        response.setUserId(session.getUserId());
        response.setCurrentStep(session.getCurrentStep());
        response.setCompleted(false);
        response.setCompanyInfo(session.getStep1() != null ? toCompanyDto(session.getStep1(), locale) : null);
        response.setContactInfo(session.getStep2() != null ? toContactDto(session.getStep2()) : null);
        response.setTemplateInfo(session.getStep3() != null ? toTemplateDto(session.getStep3()) : null);

        response.setRecords(localizeRecords(session.toRecordDtos(), locale));
        response.setRecords(session.toRecordDtos());
      
        Step3Data step3 = session.getStep3();
        return populateAvailableVariables(response, preferredLanguage, step3 != null ? step3.language : null);
    }

    private OnboardingStateResponse buildCompletedState(String userId,
                                                        String companyId,
                                                        Step1Data step1,
                                                        Step2Data step2,
                                                        Step3Data step3,
                                                        List<OnboardingStepRecordDto> records,
                                                        String preferredLanguage) {
        Locale locale = resolveLocale(preferredLanguage);

        OnboardingStateResponse response = new OnboardingStateResponse();
        response.setUserId(userId);
        response.setCompanyId(companyId);
        response.setCurrentStep(4);
        response.setCompleted(true);
        response.setCompanyInfo(toCompanyDto(step1, locale));
        response.setContactInfo(toContactDto(step2));
        response.setTemplateInfo(toTemplateDto(step3));

        response.setRecords(localizeRecords(records, locale));
        response.setRecords(records);

        return populateAvailableVariables(response, preferredLanguage, step3 != null ? step3.language : null);
    }

    private OnboardingStateResponse buildCompletedStateFromPersistence(String userId,
                                                                       CompanyProfileEntity profile,
                                                                       CompanyContactEntity contact,
                                                                       InvitationTemplateEntity template,
                                                                       String preferredLanguage) {

        Locale locale = resolveLocale(preferredLanguage);
        OnboardingStateResponse response = new OnboardingStateResponse();
        response.setUserId(userId);
        response.setCompanyId(profile.getCompanyId());
        response.setCurrentStep(4);
        response.setCompleted(true);
        response.setCompanyInfo(toCompanyDto(profile, locale));
        if (contact != null) {
            response.setContactInfo(toContactDto(contact));
        }
        if (template != null) {
            response.setTemplateInfo(toTemplateDto(template));
        }
        response.setRecords(Collections.emptyList());
        return populateAvailableVariables(response,
                preferredLanguage,
                template != null ? template.getLanguage() : null);
    }

    private OnboardingStateResponse populateAvailableVariables(OnboardingStateResponse response,
                                                               String preferredLanguage,
                                                               String templateLanguage) {
        List<String> available = resolveAvailableVariables(preferredLanguage, templateLanguage);
        response.setAvailableVariables(new ArrayList<>(available));
        return response;
    }

    private List<OnboardingStepRecordDto> localizeRecords(List<OnboardingStepRecordDto> records, Locale locale) {
        if (records == null || records.isEmpty()) {
            return records == null ? Collections.emptyList() : records;
        }
        List<OnboardingStepRecordDto> localized = new ArrayList<>(records.size());
        for (OnboardingStepRecordDto record : records) {
            if (record == null) {
                continue;
            }
            Map<String, Object> payload = record.getPayload();
            if (record.getStep() == 1 && payload != null) {
                Map<String, Object> enriched = new LinkedHashMap<>(payload);
                String countryCode = payload.get("country") instanceof String ? (String) payload.get("country") : null;
                if (StringUtils.hasText(countryCode)) {
                    locationCatalog.findCountry(countryCode, locale)
                            .ifPresent(option -> enriched.put("countryDisplayName", option.name()));
                }
                String cityCode = payload.get("city") instanceof String ? (String) payload.get("city") : null;
                if (StringUtils.hasText(countryCode) && StringUtils.hasText(cityCode)) {
                    locationCatalog.findCity(countryCode, cityCode, locale)
                            .ifPresent(option -> enriched.put("cityDisplayName", option.name()));
                }
                localized.add(new OnboardingStepRecordDto(record.getStep(), record.getSavedAt(), enriched));
            } else {
                localized.add(new OnboardingStepRecordDto(record.getStep(), record.getSavedAt(), payload));
            }
        }
        return localized;
    }

    private List<String> resolveAvailableVariables(String preferredLanguage, String templateLanguage) {
        String language = determineLanguage(preferredLanguage, templateLanguage);
        List<String> variables = AVAILABLE_TEMPLATE_VARIABLES_BY_LANGUAGE.get(language);
        if (variables == null) {
            variables = AVAILABLE_TEMPLATE_VARIABLES_BY_LANGUAGE.get(DEFAULT_TEMPLATE_LANGUAGE);
        }
        return variables;
    }


    private Locale resolveLocale(String preferredLanguage) {
        String language = determineLanguage(preferredLanguage, null);
        return switch (language) {
            case "en" -> Locale.ENGLISH;
            case "jp" -> Locale.JAPANESE;
            default -> Locale.SIMPLIFIED_CHINESE;
        };
    }

    private String determineLanguage(String primaryCandidate, String secondaryCandidate) {
        String normalizedPrimary = normalizeLanguage(primaryCandidate);
        if (normalizedPrimary != null) {
            return normalizedPrimary;
        }
        String normalizedSecondary = normalizeLanguage(secondaryCandidate);
        if (normalizedSecondary != null) {
            return normalizedSecondary;
        }
        return DEFAULT_TEMPLATE_LANGUAGE;
    }

    private String normalizeLanguage(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String[] segments = raw.toLowerCase(Locale.ROOT).split(",");
        for (String segment : segments) {
            String candidate = segment.trim();
            int semicolon = candidate.indexOf(';');
            if (semicolon >= 0) {
                candidate = candidate.substring(0, semicolon).trim();
            }
            int dash = candidate.indexOf('-');
            if (dash >= 0) {
                candidate = candidate.substring(0, dash);
            }
            int underscore = candidate.indexOf('_');
            if (underscore >= 0) {
                candidate = candidate.substring(0, underscore);
            }
            if (AVAILABLE_TEMPLATE_VARIABLES_BY_LANGUAGE.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeCountryCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCityCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
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
        profile.setCountryCode(data.countryCode);
        profile.setCityCode(data.cityCode);
        profile.setEmployeeScale(data.employeeScale);
        profile.setAnnualHiringPlan(data.annualHiringPlan);
        profile.setIndustry(data.industry);
        profile.setWebsite(data.website);
        profile.setDescription(data.description);
        profile.setDetailedAddress(data.detailedAddress);
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

    private EnterpriseCompanyInfoDto toCompanyDto(Step1Data data, Locale locale) {
        if (data == null) {
            return null;
        }
        EnterpriseCompanyInfoDto dto = new EnterpriseCompanyInfoDto();
        dto.setCompanyName(data.companyName);
        dto.setCompanyShortName(data.companyShortName);
        dto.setSocialCreditCode(data.socialCreditCode);
        dto.setCountry(data.countryCode);
        dto.setCity(data.cityCode);
        dto.setEmployeeScale(data.employeeScale);
        dto.setAnnualHiringPlan(data.annualHiringPlan);
        dto.setIndustry(data.industry);
        dto.setWebsite(data.website);
        dto.setDescription(data.description);
        dto.setDetailedAddress(data.detailedAddress);
        dto.setRecruitingPositions(data.recruitingPositions);
        locationCatalog.findCountry(data.countryCode, locale)
                .ifPresent(option -> dto.setCountryDisplayName(option.name()));
        if (!StringUtils.hasText(dto.getCountryDisplayName())) {
            dto.setCountryDisplayName(data.countryDisplayName);
        }
        locationCatalog.findCity(data.countryCode, data.cityCode, locale)
                .ifPresent(option -> dto.setCityDisplayName(option.name()));
        if (!StringUtils.hasText(dto.getCityDisplayName())) {
            dto.setCityDisplayName(data.cityDisplayName);
        }
        return dto;
    }

    private EnterpriseCompanyInfoDto toCompanyDto(CompanyProfileEntity entity, Locale locale) {
        EnterpriseCompanyInfoDto dto = new EnterpriseCompanyInfoDto();
        dto.setCompanyName(entity.getCompanyName());
        dto.setCompanyShortName(entity.getCompanyShortName());
        dto.setSocialCreditCode(entity.getSocialCreditCode());
        dto.setCountry(entity.getCountryCode());
        dto.setCity(entity.getCityCode());
        dto.setEmployeeScale(entity.getEmployeeScale());
        dto.setAnnualHiringPlan(entity.getAnnualHiringPlan());
        dto.setIndustry(entity.getIndustry());
        dto.setWebsite(entity.getWebsite());
        dto.setDescription(entity.getDescription());
        dto.setDetailedAddress(entity.getDetailedAddress());
        dto.setRecruitingPositions(loadRecruitingPositions(entity.getCompanyId()));
        locationCatalog.findCountry(entity.getCountryCode(), locale)
                .ifPresent(option -> dto.setCountryDisplayName(option.name()));
        locationCatalog.findCity(entity.getCountryCode(), entity.getCityCode(), locale)
                .ifPresent(option -> dto.setCityDisplayName(option.name()));
        if (!StringUtils.hasText(dto.getCountryDisplayName())) {
            dto.setCountryDisplayName(entity.getCountryCode());
        }
        if (!StringUtils.hasText(dto.getCityDisplayName())) {
            dto.setCityDisplayName(entity.getCityCode());
        }
        return dto;
    }

    private List<String> loadRecruitingPositions(String companyId) {
        if (recruitingPositionRepository == null || !StringUtils.hasText(companyId)) {
            return Collections.emptyList();
        }
        return recruitingPositionRepository.findByCompanyId(companyId).stream()
                .map(CompanyRecruitingPositionEntity::getPositionName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private List<String> sanitizeRecruitingPositions(List<String> positions) {
        if (positions == null || positions.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sanitized = positions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .limit(50)
                .collect(Collectors.toList());
        return Collections.unmodifiableList(sanitized);
    }

    private List<CompanyRecruitingPositionEntity> createRecruitingPositionEntities(List<String> positions,
                                                                                  String companyId,
                                                                                  Instant now) {
        if (positions == null || positions.isEmpty()) {
            return Collections.emptyList();
        }
        List<CompanyRecruitingPositionEntity> entities = new ArrayList<>(positions.size());
        for (String name : positions) {
            CompanyRecruitingPositionEntity entity = new CompanyRecruitingPositionEntity();
            entity.setPositionId(UUID.randomUUID().toString());
            entity.setCompanyId(companyId);
            entity.setPositionName(name);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entities.add(entity);
        }
        return entities;
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
        dto.setVariables(new ArrayList<>(data.variables));
        return dto;
    }

    private EnterpriseTemplateDto toTemplateDto(InvitationTemplateEntity entity) {
        EnterpriseTemplateDto dto = new EnterpriseTemplateDto();
        dto.setTemplateName(entity.getTemplateName());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setLanguage(entity.getLanguage());
        dto.setVariables(extractVariables(entity.getSubject(), entity.getBody(), entity.getLanguage()));
        return dto;
    }

    private List<String> extractVariables(String subject, String body, String language) {
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
        String resolvedLanguage = determineLanguage(language, null);
        List<String> allowedVariables = AVAILABLE_TEMPLATE_VARIABLES_BY_LANGUAGE.get(resolvedLanguage);
        for (String variable : variables) {
            if (!allowedVariables.contains(variable)) {
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
                             String countryCode,
                             String countryDisplayName,
                             String cityCode,
                             String cityDisplayName,
                             EmployeeScale employeeScale,
                             AnnualHiringPlan annualHiringPlan,
                             String industry,
                             String website,
                             String description,
                             String detailedAddress,
                             List<String> recruitingPositions) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("companyName", companyName);
            map.put("companyShortName", companyShortName);
            map.put("socialCreditCode", socialCreditCode);
            map.put("country", countryCode);
            map.put("countryDisplayName", countryDisplayName);
            map.put("city", cityCode);
            map.put("cityDisplayName", cityDisplayName);
            map.put("employeeScale", employeeScale != null ? employeeScale.name() : null);
            map.put("annualHiringPlan", annualHiringPlan != null ? annualHiringPlan.name() : null);
            map.put("industry", industry);
            map.put("website", website);
            map.put("description", description);
            map.put("detailedAddress", detailedAddress);
            map.put("recruitingPositions", recruitingPositions);
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
