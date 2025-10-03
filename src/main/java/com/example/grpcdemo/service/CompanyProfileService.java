package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.CallingCodeDto;
import com.example.grpcdemo.controller.dto.CompanyInfoUpdateRequest;
import com.example.grpcdemo.controller.dto.CompanyProfileResponse;
import com.example.grpcdemo.controller.dto.CreateHrRequest;
import com.example.grpcdemo.controller.dto.EnterpriseCompanyInfoDto;
import com.example.grpcdemo.controller.dto.HrContactDto;
import com.example.grpcdemo.controller.dto.HrContactResponse;
import com.example.grpcdemo.controller.dto.HrContactUpdateRequest;
import com.example.grpcdemo.controller.dto.PasswordSuggestionResponse;
import com.example.grpcdemo.entity.CompanyContactEntity;
import com.example.grpcdemo.entity.CompanyProfileEntity;
import com.example.grpcdemo.entity.CompanyRecruitingPositionEntity;
import com.example.grpcdemo.entity.RecruitingPositionSource;
import com.example.grpcdemo.entity.RecruitingPositionStatus;
import com.example.grpcdemo.entity.UserAccountEntity;
import com.example.grpcdemo.entity.UserAccountStatus;
import com.example.grpcdemo.location.LocationCatalog;
import com.example.grpcdemo.repository.CompanyContactRepository;
import com.example.grpcdemo.repository.CompanyProfileRepository;
import com.example.grpcdemo.repository.CompanyRecruitingPositionRepository;
import com.example.grpcdemo.repository.UserAccountRepository;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service exposing enterprise profile maintenance operations used by the
 * portal once onboarding has completed.
 */
@Service
public class CompanyProfileService {

    private static final String COMPANY_ROLE = "company";
    private static final int PASSWORD_LENGTH = 12;

    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SYMBOLS = "!@#$%^&*()_-+=[]{}".toCharArray();
    private static final char[] PASSWORD_POOL;

    static {
        StringBuilder builder = new StringBuilder();
        builder.append(UPPER);
        builder.append(LOWER);
        builder.append(DIGITS);
        builder.append(SYMBOLS);
        PASSWORD_POOL = builder.toString().toCharArray();
    }

    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyContactRepository companyContactRepository;
    private final CompanyRecruitingPositionRepository recruitingPositionRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocationCatalog locationCatalog;
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private final SecureRandom secureRandom = new SecureRandom();

    public CompanyProfileService(CompanyProfileRepository companyProfileRepository,
                                 CompanyContactRepository companyContactRepository,
                                 CompanyRecruitingPositionRepository recruitingPositionRepository,
                                 UserAccountRepository userAccountRepository,
                                 PasswordEncoder passwordEncoder,
                                 LocationCatalog locationCatalog) {
        this.companyProfileRepository = companyProfileRepository;
        this.companyContactRepository = companyContactRepository;
        this.recruitingPositionRepository = recruitingPositionRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.locationCatalog = locationCatalog;
    }

    @Transactional
    public CompanyProfileResponse updateCompanyInfo(CompanyInfoUpdateRequest request, String preferredLanguage) {
        CompanyProfileEntity profile = requireProfileByOwner(request.getUserId());
        Instant now = Instant.now();

        profile.setCompanyName(request.getCompanyName());
        profile.setCompanyShortName(request.getCompanyShortName());
        profile.setSocialCreditCode(request.getSocialCreditCode());
        profile.setCountryCode(normalizeCode(request.getCountry()));
        profile.setCityCode(normalizeCode(request.getCity()));
        profile.setEmployeeScale(request.getEmployeeScale());
        profile.setAnnualHiringPlan(request.getAnnualHiringPlan());
        profile.setIndustry(trimToNull(request.getIndustry()));
        profile.setWebsite(trimToNull(request.getWebsite()));
        profile.setDescription(trimToNull(request.getDescription()));
        profile.setDetailedAddress(trimToNull(request.getDetailedAddress()));
        profile.setUpdatedAt(now);

        companyProfileRepository.save(profile);
        replaceRecruitingPositions(profile.getCompanyId(), request.getRecruitingPositions(), now);

        Locale locale = resolveLocale(preferredLanguage);
        return buildProfileResponse(profile, locale);
    }

    @Transactional
    public HrContactResponse createContact(CreateHrRequest request) {
        CompanyProfileEntity profile = requireProfileByOwner(request.getUserId());

        ensureEmailAvailable(request.getContactEmail(), null);

        String plainPassword = StringUtils.hasText(request.getPassword())
                ? request.getPassword()
                : generateStrongPassword();

        String userId = UUID.randomUUID().toString();
        UserAccountEntity account = new UserAccountEntity();
        account.setUserId(userId);
        account.setEmail(request.getContactEmail());
        account.setPasswordHash(passwordEncoder.encode(plainPassword));
        account.setRole(COMPANY_ROLE);
        account.setStatus(UserAccountStatus.ACTIVE);
        userAccountRepository.save(account);

        CompanyContactEntity contact = new CompanyContactEntity();
        contact.setContactId(UUID.randomUUID().toString());
        contact.setCompanyId(profile.getCompanyId());
        contact.setUserAccountId(userId);
        contact.setContactName(request.getContactName());
        contact.setContactEmail(request.getContactEmail());
        contact.setPhoneCountryCode(request.getPhoneCountryCode());
        contact.setPhoneNumber(request.getPhoneNumber());
        contact.setPosition(trimToNull(request.getPosition()));
        contact.setDepartment(trimToNull(request.getDepartment()));
        contact.setPrimaryContact(Boolean.TRUE.equals(request.getPrimary()));
        contact.setCreatedAt(Instant.now());
        contact.setUpdatedAt(contact.getCreatedAt());

        companyContactRepository.save(contact);
        if (contact.isPrimaryContact()) {
            enforcePrimaryContact(profile.getCompanyId(), contact.getContactId());
        }

        HrContactResponse response = new HrContactResponse();
        response.setContact(toDto(contact));
        response.setPassword(plainPassword);
        return response;
    }

    @Transactional
    public HrContactResponse updateContact(String contactId, HrContactUpdateRequest request) {
        CompanyProfileEntity profile = requireProfileByOwner(request.getUserId());
        CompanyContactEntity contact = companyContactRepository
                .findByContactIdAndCompanyId(contactId, profile.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到指定的 HR 联系人"));

        boolean emailChanged = !contact.getContactEmail().equalsIgnoreCase(request.getContactEmail());
        if (emailChanged) {
            ensureEmailAvailable(request.getContactEmail(), contact.getUserAccountId());
        }

        contact.setContactName(request.getContactName());
        contact.setContactEmail(request.getContactEmail());
        contact.setPhoneCountryCode(request.getPhoneCountryCode());
        contact.setPhoneNumber(request.getPhoneNumber());
        contact.setPosition(trimToNull(request.getPosition()));
        contact.setDepartment(trimToNull(request.getDepartment()));
        if (request.getPrimary() != null) {
            contact.setPrimaryContact(request.getPrimary());
        }
        contact.setUpdatedAt(Instant.now());

        String newPassword = null;
        if (StringUtils.hasText(contact.getUserAccountId())) {
            Optional<UserAccountEntity> accountOptional = userAccountRepository.findById(contact.getUserAccountId());
            UserAccountEntity account = accountOptional.orElse(null);
            if (account != null) {
                if (emailChanged) {
                    account.setEmail(request.getContactEmail());
                }
                if (StringUtils.hasText(request.getNewPassword())) {
                    newPassword = request.getNewPassword();
                    account.setPasswordHash(passwordEncoder.encode(newPassword));
                }
                userAccountRepository.save(account);
            }
        } else if (StringUtils.hasText(request.getNewPassword())) {
            newPassword = request.getNewPassword();
            UserAccountEntity account = new UserAccountEntity();
            String newUserId = UUID.randomUUID().toString();
            account.setUserId(newUserId);
            account.setEmail(request.getContactEmail());
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            account.setRole(COMPANY_ROLE);
            account.setStatus(UserAccountStatus.ACTIVE);
            userAccountRepository.save(account);
            contact.setUserAccountId(newUserId);
        }

        companyContactRepository.save(contact);
        if (Boolean.TRUE.equals(request.getPrimary())) {
            enforcePrimaryContact(profile.getCompanyId(), contact.getContactId());
        }

        HrContactResponse response = new HrContactResponse();
        response.setContact(toDto(contact));
        response.setPassword(newPassword);
        return response;
    }

    @Transactional
    public CompanyProfileResponse getProfile(String userId, String preferredLanguage) {
        CompanyProfileEntity profile = requireProfileByOwner(userId);
        Locale locale = resolveLocale(preferredLanguage);
        return buildProfileResponse(profile, locale);
    }

    @Transactional
    public List<CallingCodeDto> listCallingCodes(String preferredLanguage) {
        Locale locale = resolveLocale(preferredLanguage);
        Set<String> regions = phoneNumberUtil.getSupportedRegions();
        List<CallingCodeDto> options = new ArrayList<>(regions.size());
        for (String region : regions) {
            int code = phoneNumberUtil.getCountryCodeForRegion(region);
            if (code <= 0) {
                continue;
            }
            String callingCode = "+" + code;
            Locale countryLocale = new Locale("", region);
            String displayName = countryLocale.getDisplayCountry(locale);
            if (!StringUtils.hasText(displayName)) {
                displayName = countryLocale.getDisplayCountry(Locale.ENGLISH);
            }
            options.add(new CallingCodeDto(region, displayName, callingCode));
        }
        options.sort(Comparator.comparing(CallingCodeDto::getCountryName, Comparator.nullsLast(String::compareTo))
                .thenComparing(CallingCodeDto::getCallingCode));
        return options;
    }

    public PasswordSuggestionResponse generatePassword() {
        return new PasswordSuggestionResponse(generateStrongPassword());
    }

    private CompanyProfileEntity requireProfileByOwner(String userId) {
        return companyProfileRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到企业档案"));
    }

    private CompanyProfileResponse buildProfileResponse(CompanyProfileEntity profile, Locale locale) {
        CompanyProfileResponse response = new CompanyProfileResponse();
        response.setCompanyId(profile.getCompanyId());
        response.setCompany(toCompanyDto(profile, locale));
        response.setHrContacts(loadContacts(profile.getCompanyId()));
        return response;
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
        return dto;
    }

    private List<String> loadRecruitingPositions(String companyId) {
        return recruitingPositionRepository.findByCompanyId(companyId).stream()
                .map(CompanyRecruitingPositionEntity::getPositionName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private List<HrContactDto> loadContacts(String companyId) {
        return companyContactRepository.findByCompanyIdOrderByCreatedAtAsc(companyId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private HrContactDto toDto(CompanyContactEntity entity) {
        HrContactDto dto = new HrContactDto();
        dto.setContactId(entity.getContactId());
        dto.setUserAccountId(entity.getUserAccountId());
        dto.setContactName(entity.getContactName());
        dto.setContactEmail(entity.getContactEmail());
        dto.setPhoneCountryCode(entity.getPhoneCountryCode());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setPosition(entity.getPosition());
        dto.setDepartment(entity.getDepartment());
        dto.setPrimary(entity.isPrimaryContact());
        return dto;
    }

    private void replaceRecruitingPositions(String companyId, List<String> positions, Instant now) {
        List<String> sanitized = sanitizePositions(positions);
        List<CompanyRecruitingPositionEntity> existing = recruitingPositionRepository.findByCompanyId(companyId);
        if (!existing.isEmpty()) {
            recruitingPositionRepository.deleteAll(existing);
        }
        if (!sanitized.isEmpty()) {
            List<CompanyRecruitingPositionEntity> entities = sanitized.stream()
                    .map(name -> {
                        CompanyRecruitingPositionEntity record = new CompanyRecruitingPositionEntity();
                        record.setPositionId(UUID.randomUUID().toString());
                        record.setCompanyId(companyId);
                        record.setPositionName(name);
                        record.setPositionLocation(null);
                        record.setPublisherNickname(null);
                        record.setStatus(RecruitingPositionStatus.READY);
                        record.setSource(RecruitingPositionSource.MANUAL);
                        record.setCreatedAt(now);
                        record.setUpdatedAt(now);
                        return record;
                    })
                    .toList();
            recruitingPositionRepository.saveAll(entities);
        }
    }

    private List<String> sanitizePositions(List<String> positions) {
        if (positions == null || positions.isEmpty()) {
            return List.of();
        }
        return positions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .limit(50)
                .toList();
    }

    private void enforcePrimaryContact(String companyId, String primaryContactId) {
        List<CompanyContactEntity> contacts = companyContactRepository.findByCompanyId(companyId);
        for (CompanyContactEntity entity : contacts) {
            boolean shouldBePrimary = entity.getContactId().equals(primaryContactId);
            if (entity.isPrimaryContact() != shouldBePrimary) {
                entity.setPrimaryContact(shouldBePrimary);
                entity.setUpdatedAt(Instant.now());
                companyContactRepository.save(entity);
            }
        }
    }

    private void ensureEmailAvailable(String email, String currentUserId) {
        Optional<UserAccountEntity> existing = userAccountRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            if (currentUserId == null || !existing.get().getUserId().equals(currentUserId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被占用");
            }
        }
    }

    private String generateStrongPassword() {
        List<Character> characters = new ArrayList<>(PASSWORD_LENGTH);
        characters.add(randomChar(UPPER));
        characters.add(randomChar(LOWER));
        characters.add(randomChar(DIGITS));
        characters.add(randomChar(SYMBOLS));
        while (characters.size() < PASSWORD_LENGTH) {
            characters.add(randomChar(PASSWORD_POOL));
        }
        java.util.Collections.shuffle(characters, secureRandom);
        StringBuilder builder = new StringBuilder(characters.size());
        for (Character character : characters) {
            builder.append(character);
        }
        return builder.toString();
    }

    private char randomChar(char[] candidates) {
        return candidates[secureRandom.nextInt(candidates.length)];
    }

    private Locale resolveLocale(String preferredLanguage) {
        String language = normalizeLanguage(preferredLanguage);
        if (language == null) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return switch (language) {
            case "en" -> Locale.ENGLISH;
            case "jp" -> Locale.JAPANESE;
            default -> Locale.SIMPLIFIED_CHINESE;
        };
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
            if (candidate.equals("zh") || candidate.equals("en") || candidate.equals("jp")) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
