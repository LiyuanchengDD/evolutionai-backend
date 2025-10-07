package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.CandidateInterviewAbandonRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewAnswerRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewAnswerResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewDetailResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewCompleteRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewInvitationItem;
import com.example.grpcdemo.controller.dto.CandidateInterviewInvitationListResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewPrecheckDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewPrecheckRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewProfilePhotoDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewProfilePhotoRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewQuestionDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewRecordResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewStartRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewStartResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewStatusCounter;
import com.example.grpcdemo.controller.dto.CandidateInterviewTechnicalRequirementDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewAudioDto;
import com.example.grpcdemo.entity.CandidateInterviewAudioEntity;
import com.example.grpcdemo.entity.CandidateInterviewRecordEntity;
import com.example.grpcdemo.entity.CompanyContactEntity;
import com.example.grpcdemo.entity.CompanyJobCandidateEntity;
import com.example.grpcdemo.entity.CompanyProfileEntity;
import com.example.grpcdemo.entity.CompanyRecruitingPositionEntity;
import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import com.example.grpcdemo.location.LocationCatalog;
import com.example.grpcdemo.repository.CandidateInterviewAudioRepository;
import com.example.grpcdemo.repository.CandidateInterviewRecordRepository;
import com.example.grpcdemo.repository.CompanyContactRepository;
import com.example.grpcdemo.repository.CompanyJobCandidateRepository;
import com.example.grpcdemo.repository.CompanyProfileRepository;
import com.example.grpcdemo.repository.CompanyRecruitingPositionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 候选人智能面试门户核心业务。
 */
@Service
public class CandidateInterviewPortalService {

    private static final String STATUS_ALL = "ALL";
    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ABANDONED = "ABANDONED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_TIMED_OUT = "TIMED_OUT";

    private static final Duration ANSWER_TIME_LIMIT = Duration.ofMinutes(60);

    private static final Map<String, Set<JobCandidateInterviewStatus>> STATUS_GROUPS = Map.of(
            STATUS_WAITING, EnumSet.of(JobCandidateInterviewStatus.NOT_INTERVIEWED,
                    JobCandidateInterviewStatus.SCHEDULED),
            STATUS_IN_PROGRESS, EnumSet.of(JobCandidateInterviewStatus.IN_PROGRESS),
            STATUS_COMPLETED, EnumSet.of(JobCandidateInterviewStatus.COMPLETED),
            STATUS_ABANDONED, EnumSet.of(JobCandidateInterviewStatus.ABANDONED),
            STATUS_CANCELLED, EnumSet.of(JobCandidateInterviewStatus.CANCELLED),
            STATUS_TIMED_OUT, EnumSet.of(JobCandidateInterviewStatus.TIMED_OUT)
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
            STATUS_ALL, "全部",
            STATUS_WAITING, "待面试",
            STATUS_IN_PROGRESS, "进行中",
            STATUS_COMPLETED, "已面试",
            STATUS_ABANDONED, "已放弃",
            STATUS_CANCELLED, "已取消",
            STATUS_TIMED_OUT, "已超时"
    );

    private static final TypeReference<List<CandidateInterviewQuestionDto>> QUESTION_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> GENERIC_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> TRANSCRIPT_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<CandidateInterviewTechnicalRequirementDto>> REQUIREMENT_LIST_TYPE = new TypeReference<>() {
    };

    private final CompanyJobCandidateRepository candidateRepository;
    private final CompanyRecruitingPositionRepository positionRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyContactRepository contactRepository;
    private final CandidateInterviewRecordRepository interviewRecordRepository;
    private final CandidateInterviewAudioRepository interviewAudioRepository;
    private final InterviewQuestionClient questionClient;
    private final ObjectMapper objectMapper;
    private final LocationCatalog locationCatalog;

    public CandidateInterviewPortalService(CompanyJobCandidateRepository candidateRepository,
                                           CompanyRecruitingPositionRepository positionRepository,
                                           CompanyProfileRepository companyProfileRepository,
                                           CompanyContactRepository contactRepository,
                                           CandidateInterviewRecordRepository interviewRecordRepository,
                                           CandidateInterviewAudioRepository interviewAudioRepository,
                                           InterviewQuestionClient questionClient,
                                           ObjectMapper objectMapper,
                                           LocationCatalog locationCatalog) {
        this.candidateRepository = candidateRepository;
        this.positionRepository = positionRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.contactRepository = contactRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewAudioRepository = interviewAudioRepository;
        this.questionClient = questionClient;
        this.objectMapper = objectMapper;
        this.locationCatalog = locationCatalog;
    }

    @Transactional
    public CandidateInterviewInvitationListResponse listInvitations(String jobCandidateId,
                                                                   String statusCode,
                                                                   String keyword,
                                                                   Locale locale) {
        CompanyJobCandidateEntity anchor = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        List<CompanyJobCandidateEntity> group = loadCandidateGroup(anchor);
        if (group.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在");
        }
        Map<String, CompanyRecruitingPositionEntity> positions = loadPositions(group);
        Map<String, CompanyProfileEntity> companies = loadCompanies(positions.values());
        Map<String, CompanyContactEntity> contacts = loadContacts(companies.keySet());
        Map<String, CandidateInterviewRecordEntity> records = loadLatestRecords(group);

        Locale targetLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        List<CandidateInterviewInvitationItem> items = group.stream()
                .map(candidate -> toInvitationItem(candidate,
                        positions.get(candidate.getPositionId()),
                        companies,
                        contacts,
                        records.get(candidate.getJobCandidateId()),
                        targetLocale))
                .filter(Objects::nonNull)
                .filter(item -> matchesKeyword(item, keyword))
                .collect(Collectors.toList());

        String normalizedStatus = normalizeStatus(statusCode);
        if (!STATUS_ALL.equals(normalizedStatus)) {
            Set<JobCandidateInterviewStatus> statuses = STATUS_GROUPS.getOrDefault(normalizedStatus, Set.of());
            items = items.stream()
                    .filter(item -> statuses.contains(item.getInterviewStatus()))
                    .collect(Collectors.toList());
        }

        CandidateInterviewInvitationListResponse response = new CandidateInterviewInvitationListResponse();
        response.setInvitations(items);
        response.setStatusCounters(buildStatusCounters(group));
        response.setTotal(items.size());
        response.setActiveStatus(normalizedStatus);
        response.setKeyword(keyword);
        return response;
    }

    @Transactional
    public CandidateInterviewDetailResponse getInvitationDetail(String jobCandidateId, Locale locale) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        CompanyRecruitingPositionEntity position = positionRepository.findById(candidate.getPositionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
        CompanyProfileEntity company = companyProfileRepository.findById(position.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "企业不存在"));
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElse(null);
        record = ensureActiveInterviewWindow(candidate, record, false);
        List<CandidateInterviewAudioEntity> audios = record == null ? Collections.emptyList()
                : interviewAudioRepository
                .findByJobCandidateIdAndInterviewRecordIdOrderByQuestionSequenceAscCreatedAtAsc(jobCandidateId, record.getRecordId());

        Map<String, CompanyProfileEntity> companyMap = Map.of(company.getCompanyId(), company);
        Map<String, CompanyContactEntity> contactMap = new HashMap<>();
        contactRepository.findFirstByCompanyIdOrderByCreatedAtAsc(company.getCompanyId())
                .ifPresent(contact -> contactMap.put(company.getCompanyId(), contact));

        CandidateInterviewDetailResponse response = new CandidateInterviewDetailResponse();
        response.setInvitation(toInvitationItem(candidate,
                position,
                companyMap,
                contactMap,
                record,
                locale != null ? locale : Locale.SIMPLIFIED_CHINESE));
        response.setRecord(toRecordResponse(record, candidate, position, audios));
        response.setRequirements(extractRequirements(record));
        response.setPrecheckPassed(record != null && "PASSED".equalsIgnoreCase(record.getPrecheckStatus()));
        response.setProfilePhotoUploaded(record != null && record.getProfilePhotoData() != null);
        response.setAnswerDeadlineAt(record != null ? record.getAnswerDeadlineAt() : null);
        response.setReadyForInterview(response.isPrecheckPassed() && response.isProfilePhotoUploaded());
        return response;
    }

    @Transactional
    public CandidateInterviewDetailResponse submitPrecheck(String jobCandidateId,
                                                           CandidateInterviewPrecheckRequest request,
                                                           Locale locale) {
        if (request == null || request.getSummary() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少预检结果");
        }
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseGet(() -> {
                    CandidateInterviewRecordEntity entity = new CandidateInterviewRecordEntity();
                    entity.setRecordId(UUID.randomUUID().toString());
                    entity.setJobCandidateId(jobCandidateId);
                    return entity;
                });

        CandidateInterviewPrecheckDto summary = request.getSummary();
        record.setPrecheckStatus(summary.getStatus());
        if (summary.getCompletedAt() != null) {
            record.setPrecheckCompletedAt(summary.getCompletedAt());
        } else {
            record.setPrecheckCompletedAt(Instant.now());
        }
        record.setPrecheckReportJson(writeJson(Map.of(
                "summary", summary,
                "requirements", Optional.ofNullable(request.getRequirements()).orElse(List.of())
        ), "序列化预检报告失败"));
        CandidateInterviewRecordEntity saved = interviewRecordRepository.save(record);

        candidate.setInterviewStatus(JobCandidateInterviewStatus.SCHEDULED);
        candidate.setCandidateResponseAt(Instant.now());
        candidate.setInterviewRecordId(saved.getRecordId());
        candidate.setUpdatedAt(Instant.now());
        candidateRepository.save(candidate);

        return getInvitationDetail(jobCandidateId, locale);
    }

    @Transactional
    public CandidateInterviewStartResponse startInterview(String jobCandidateId,
                                                          CandidateInterviewStartRequest request) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        if (candidate.getInterviewStatus() == JobCandidateInterviewStatus.COMPLETED
                || candidate.getInterviewStatus() == JobCandidateInterviewStatus.ABANDONED
                || candidate.getInterviewStatus() == JobCandidateInterviewStatus.CANCELLED
                || candidate.getInterviewStatus() == JobCandidateInterviewStatus.TIMED_OUT) {
            throw new ResponseStatusException(HttpStatus.GONE, "面试已结束，请重新预约");
        }
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "请先完成设备检测"));
        record = ensureActiveInterviewWindow(candidate, record, true);
        if (!"PASSED".equalsIgnoreCase(record.getPrecheckStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "麦克风检测未通过");
        }
        boolean needNewQuestions = !StringUtils.hasText(record.getQuestionsJson())
                || (request != null && request.isRefreshQuestions());
        List<CandidateInterviewQuestionDto> questions;
        Map<String, Object> metadata;
        if (needNewQuestions) {
            CompanyRecruitingPositionEntity position = positionRepository.findById(candidate.getPositionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
            CompanyProfileEntity company = companyProfileRepository.findById(position.getCompanyId())
                    .orElse(null);
            InterviewQuestionClient.InterviewQuestionSet set = questionClient.fetchQuestions(
                    new InterviewQuestionClient.InterviewQuestionCommand(jobCandidateId,
                            position.getPositionId(),
                            candidate.getCandidateName(),
                            position.getPositionName(),
                            company != null ? company.getCompanyName() : null,
                            request != null ? request.getLocale() : null,
                            request != null ? request.getContext() : Map.of()));
            record.setAiSessionId(set.sessionId());
            record.setQuestionsJson(writeJson(set.questions(), "序列化面试题目失败"));
            record.setMetadataJson(writeJson(set.metadata(), "序列化面试元数据失败"));
            questions = set.questions();
            metadata = set.metadata();
        } else {
            questions = readQuestionList(record.getQuestionsJson());
            metadata = readGenericMap(record.getMetadataJson());
        }
        Instant now = Instant.now();
        if (record.getInterviewStartedAt() == null) {
            record.setInterviewStartedAt(now);
        }
        if (record.getAnswerDeadlineAt() == null && record.getInterviewStartedAt() != null) {
            record.setAnswerDeadlineAt(record.getInterviewStartedAt().plus(ANSWER_TIME_LIMIT));
        }
        if (!StringUtils.hasText(record.getInterviewMode())) {
            record.setInterviewMode("AI_VIDEO");
        }
        record = interviewRecordRepository.save(record);

        candidate.setInterviewStatus(JobCandidateInterviewStatus.IN_PROGRESS);
        candidate.setInterviewRecordId(record.getRecordId());
        candidate.setUpdatedAt(now);
        candidateRepository.save(candidate);

        CandidateInterviewStartResponse response = new CandidateInterviewStartResponse();
        response.setInterviewRecordId(record.getRecordId());
        response.setAiSessionId(record.getAiSessionId());
        response.setInterviewStartedAt(record.getInterviewStartedAt());
        response.setAnswerDeadlineAt(record.getAnswerDeadlineAt());
        response.setQuestions(questions);
        response.setMetadata(metadata);
        return response;
    }

    @Transactional
    public CandidateInterviewAnswerResponse submitAnswer(String jobCandidateId,
                                                         CandidateInterviewAnswerRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少作答内容");
        }
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "尚未创建面试记录"));
        record = ensureActiveInterviewWindow(candidate, record, true);
        if (record.getProfilePhotoData() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "请先上传个人照片");
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(request.getBase64Content());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "音频内容不是有效的 Base64 编码", e);
        }

        CandidateInterviewAudioEntity audio = resolveAudioEntity(record, request, jobCandidateId);
        audio.setContentType(StringUtils.hasText(request.getContentType()) ? request.getContentType() : "audio/mpeg");
        audio.setFileName(StringUtils.hasText(request.getFileName()) ? request.getFileName() : buildAudioFileName(candidate, request.getQuestionSequence()));
        audio.setDurationSeconds(request.getDurationSeconds());
        audio.setSizeBytes((long) data.length);
        audio.setTranscript(request.getTranscript());
        audio.setAudioData(data);
        interviewAudioRepository.save(audio);

        record.setCurrentQuestionSequence(request.getQuestionSequence());
        record.setTranscriptJson(updateTranscript(record.getTranscriptJson(), request));
        record = interviewRecordRepository.save(record);

        Instant now = Instant.now();
        candidate.setInterviewStatus(JobCandidateInterviewStatus.IN_PROGRESS);
        candidate.setUpdatedAt(now);
        candidateRepository.save(candidate);

        CandidateInterviewAnswerResponse response = new CandidateInterviewAnswerResponse();
        CompanyRecruitingPositionEntity position = positionRepository.findById(candidate.getPositionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
        List<CandidateInterviewAudioEntity> audios = interviewAudioRepository
                .findByJobCandidateIdAndInterviewRecordIdOrderByQuestionSequenceAscCreatedAtAsc(jobCandidateId, record.getRecordId());
        response.setRecord(toRecordResponse(record, candidate, position, audios));
        response.setAudio(toAudioDto(audio));
        return response;
    }

    @Transactional
    public CandidateInterviewRecordResponse completeInterview(String jobCandidateId,
                                                              CandidateInterviewCompleteRequest request) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "尚未创建面试记录"));
        record = ensureActiveInterviewWindow(candidate, record, true);
        Instant now = Instant.now();
        record.setInterviewEndedAt(now);
        if (request != null && request.getDurationSeconds() != null) {
            record.setDurationSeconds(request.getDurationSeconds());
        } else if (record.getInterviewStartedAt() != null) {
            record.setDurationSeconds(Math.toIntExact(ChronoUnit.SECONDS.between(record.getInterviewStartedAt(), now)));
        }
        if (request != null && request.getMetadata() != null) {
            Map<String, Object> metadata = new HashMap<>(readGenericMap(record.getMetadataJson()));
            metadata.put("completion", request.getMetadata());
            record.setMetadataJson(writeJson(metadata, "序列化面试元数据失败"));
        }
        CandidateInterviewRecordEntity saved = interviewRecordRepository.save(record);

        candidate.setInterviewStatus(JobCandidateInterviewStatus.COMPLETED);
        candidate.setInterviewCompletedAt(now);
        candidate.setUpdatedAt(now);
        candidateRepository.save(candidate);

        CompanyRecruitingPositionEntity position = positionRepository.findById(candidate.getPositionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
        List<CandidateInterviewAudioEntity> audios = interviewAudioRepository
                .findByJobCandidateIdAndInterviewRecordIdOrderByQuestionSequenceAscCreatedAtAsc(jobCandidateId, saved.getRecordId());
        return toRecordResponse(saved, candidate, position, audios);
    }

    @Transactional
    public CandidateInterviewInvitationItem abandonInterview(String jobCandidateId,
                                                             CandidateInterviewAbandonRequest request,
                                                             Locale locale) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElse(null);
        Instant now = Instant.now();
        candidate.setInterviewStatus(JobCandidateInterviewStatus.ABANDONED);
        candidate.setCandidateResponseAt(now);
        candidate.setInterviewCompletedAt(now);
        candidate.setUpdatedAt(now);
        candidateRepository.save(candidate);

        if (record != null && request != null) {
            Map<String, Object> metadata = new HashMap<>(readGenericMap(record.getMetadataJson()));
            metadata.put("abandon", Map.of(
                    "reason", request.getReason(),
                    "submittedAt", now.toString(),
                    "extra", request.getMetadata()
            ));
            record.setMetadataJson(writeJson(metadata, "序列化面试元数据失败"));
            record.setInterviewEndedAt(now);
            interviewRecordRepository.save(record);
        }

        CompanyRecruitingPositionEntity position = positionRepository.findById(candidate.getPositionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
        CompanyProfileEntity company = companyProfileRepository.findById(position.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "企业不存在"));
        CandidateInterviewInvitationItem item = new CandidateInterviewInvitationItem();
        CandidateInterviewRecordEntity latestRecord = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElse(record);
        Map<String, CompanyProfileEntity> companies = Map.of(company.getCompanyId(), company);
        Map<String, CompanyContactEntity> contacts = Map.of(company.getCompanyId(), contactRepository
                .findFirstByCompanyIdOrderByCreatedAtAsc(company.getCompanyId()).orElse(null));
        CandidateInterviewInvitationItem populated = toInvitationItem(candidate, position, companies, contacts, latestRecord,
                locale != null ? locale : Locale.SIMPLIFIED_CHINESE);
        return populated != null ? populated : item;
    }

    @Transactional
    public CandidateInterviewProfilePhotoDto uploadProfilePhoto(String jobCandidateId,
                                                                CandidateInterviewProfilePhotoRequest request) {
        if (request == null || !StringUtils.hasText(request.getBase64Content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片内容不能为空");
        }
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseGet(() -> {
                    CandidateInterviewRecordEntity entity = new CandidateInterviewRecordEntity();
                    entity.setRecordId(UUID.randomUUID().toString());
                    entity.setJobCandidateId(jobCandidateId);
                    return entity;
                });
        record = ensureActiveInterviewWindow(candidate, record, true);
        byte[] data;
        try {
            data = Base64.getDecoder().decode(request.getBase64Content());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片内容不是有效的 Base64 编码", e);
        }
        record.setProfilePhotoData(data);
        record.setProfilePhotoFileName(request.getFileName());
        record.setProfilePhotoContentType(StringUtils.hasText(request.getContentType()) ? request.getContentType() : "image/jpeg");
        record.setProfilePhotoSizeBytes(request.getSizeBytes() != null ? request.getSizeBytes() : (long) data.length);
        record.setProfilePhotoUploadedAt(Instant.now());
        CandidateInterviewRecordEntity saved = interviewRecordRepository.save(record);

        candidate.setInterviewRecordId(saved.getRecordId());
        candidate.setUpdatedAt(Instant.now());
        candidateRepository.save(candidate);

        CandidateInterviewProfilePhotoDto dto = new CandidateInterviewProfilePhotoDto();
        dto.setFileName(saved.getProfilePhotoFileName());
        dto.setContentType(saved.getProfilePhotoContentType());
        dto.setSizeBytes(saved.getProfilePhotoSizeBytes());
        dto.setUploadedAt(saved.getProfilePhotoUploadedAt());
        dto.setDownloadUrl(String.format("/api/candidate/interview-sessions/%s/profile-photo", jobCandidateId));
        return dto;
    }

    @Transactional(readOnly = true)
    public CompanyJobCandidateService.InterviewProfilePhotoPayload downloadProfilePhoto(String jobCandidateId) {
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到面试记录"));
        byte[] data = record.getProfilePhotoData();
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未上传头像");
        }
        String contentType = StringUtils.hasText(record.getProfilePhotoContentType())
                ? record.getProfilePhotoContentType()
                : "image/jpeg";
        String fileName = StringUtils.hasText(record.getProfilePhotoFileName())
                ? record.getProfilePhotoFileName()
                : "profile.jpg";
        return new CompanyJobCandidateService.InterviewProfilePhotoPayload(fileName, contentType, data);
    }

    @Transactional(readOnly = true)
    public CompanyJobCandidateService.InterviewAudioPayload downloadAudio(String jobCandidateId, String audioId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约不存在"));
        CandidateInterviewAudioEntity audio = interviewAudioRepository.findByAudioIdAndJobCandidateId(audioId, jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "音频不存在"));
        byte[] data = audio.getAudioData();
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "音频内容不存在");
        }
        String contentType = StringUtils.hasText(audio.getContentType()) ? audio.getContentType() : "audio/mpeg";
        String fileName = StringUtils.hasText(audio.getFileName()) ? audio.getFileName() : buildAudioFileName(candidate, audio.getQuestionSequence());
        return new CompanyJobCandidateService.InterviewAudioPayload(fileName, contentType, data);
    }

    private List<CompanyJobCandidateEntity> loadCandidateGroup(CompanyJobCandidateEntity anchor) {
        Set<String> seen = new LinkedHashSet<>();
        List<CompanyJobCandidateEntity> result = new ArrayList<>();
        addCandidateIfNeeded(anchor, seen, result);
        if (StringUtils.hasText(anchor.getCandidateEmail())) {
            candidateRepository.findByCandidateEmailIgnoreCase(anchor.getCandidateEmail())
                    .forEach(entity -> addCandidateIfNeeded(entity, seen, result));
        }
        if (StringUtils.hasText(anchor.getCandidatePhone())) {
            candidateRepository.findByCandidatePhone(anchor.getCandidatePhone())
                    .forEach(entity -> addCandidateIfNeeded(entity, seen, result));
        }
        return result;
    }

    private void addCandidateIfNeeded(CompanyJobCandidateEntity entity, Set<String> seen, List<CompanyJobCandidateEntity> result) {
        if (entity == null || !seen.add(entity.getJobCandidateId())) {
            return;
        }
        result.add(entity);
    }

    private Map<String, CompanyRecruitingPositionEntity> loadPositions(List<CompanyJobCandidateEntity> candidates) {
        Set<String> positionIds = candidates.stream()
                .map(CompanyJobCandidateEntity::getPositionId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        return positionRepository.findAllById(positionIds).stream()
                .collect(Collectors.toMap(CompanyRecruitingPositionEntity::getPositionId, entity -> entity));
    }

    private Map<String, CompanyProfileEntity> loadCompanies(Iterable<CompanyRecruitingPositionEntity> positions) {
        Set<String> companyIds = new LinkedHashSet<>();
        for (CompanyRecruitingPositionEntity position : positions) {
            if (position != null && StringUtils.hasText(position.getCompanyId())) {
                companyIds.add(position.getCompanyId());
            }
        }
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        return companyProfileRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(CompanyProfileEntity::getCompanyId, entity -> entity));
    }

    private Map<String, CompanyContactEntity> loadContacts(Set<String> companyIds) {
        Map<String, CompanyContactEntity> map = new HashMap<>();
        for (String companyId : companyIds) {
            if (!StringUtils.hasText(companyId)) {
                continue;
            }
            contactRepository.findFirstByCompanyIdOrderByCreatedAtAsc(companyId)
                    .ifPresent(contact -> map.put(companyId, contact));
        }
        return map;
    }

    private Map<String, CandidateInterviewRecordEntity> loadLatestRecords(List<CompanyJobCandidateEntity> candidates) {
        Map<String, CandidateInterviewRecordEntity> records = new HashMap<>();
        for (CompanyJobCandidateEntity candidate : candidates) {
            interviewRecordRepository.findFirstByJobCandidateIdOrderByCreatedAtDesc(candidate.getJobCandidateId())
                    .ifPresent(record -> records.put(candidate.getJobCandidateId(), record));
        }
        return records;
    }

    private CandidateInterviewInvitationItem toInvitationItem(CompanyJobCandidateEntity candidate,
                                                              CompanyRecruitingPositionEntity position,
                                                              Map<String, CompanyProfileEntity> companies,
                                                              Map<String, CompanyContactEntity> contacts,
                                                              CandidateInterviewRecordEntity record,
                                                              Locale locale) {
        if (position == null) {
            return null;
        }
        record = ensureActiveInterviewWindow(candidate, record, false);
        CompanyProfileEntity company = companies.get(position.getCompanyId());
        CandidateInterviewInvitationItem item = new CandidateInterviewInvitationItem();
        item.setJobCandidateId(candidate.getJobCandidateId());
        item.setPositionId(position.getPositionId());
        item.setPositionName(position.getPositionName());
        item.setCompanyId(position.getCompanyId());
        if (company != null) {
            item.setCompanyName(company.getCompanyName());
            item.setCountryCode(company.getCountryCode());
            item.setCityCode(company.getCityCode());
            locationCatalog.findCountry(company.getCountryCode(), locale).ifPresent(option -> item.setCountryName(option.name()));
            locationCatalog.findCity(company.getCountryCode(), company.getCityCode(), locale)
                    .ifPresent(option -> item.setCityName(option.name()));
            CompanyContactEntity contact = contacts.get(company.getCompanyId());
            if (contact != null) {
                item.setHrName(contact.getContactName());
                item.setHrEmail(contact.getContactEmail());
                item.setHrPhone(contact.getPhoneCountryCode() + " " + contact.getPhoneNumber());
            }
        }
        item.setInterviewStatus(candidate.getInterviewStatus());
        item.setInviteStatus(candidate.getInviteStatus());
        item.setLastInviteSentAt(candidate.getLastInviteSentAt());
        item.setInterviewDeadlineAt(candidate.getInterviewDeadlineAt());
        item.setInterviewCompletedAt(candidate.getInterviewCompletedAt());
        item.setUpdatedAt(candidate.getUpdatedAt());
        if (record != null) {
            item.setPrecheckPassed("PASSED".equalsIgnoreCase(record.getPrecheckStatus()));
            item.setProfilePhotoUploaded(record.getProfilePhotoData() != null);
            item.setAnswerDeadlineAt(record.getAnswerDeadlineAt());
        }
        return item;
    }

    private boolean matchesKeyword(CandidateInterviewInvitationItem item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return (item.getPositionName() != null && item.getPositionName().toLowerCase(Locale.ROOT).contains(normalized))
                || (item.getCompanyName() != null && item.getCompanyName().toLowerCase(Locale.ROOT).contains(normalized));
    }

    private List<CandidateInterviewStatusCounter> buildStatusCounters(List<CompanyJobCandidateEntity> candidates) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put(STATUS_ALL, (long) candidates.size());
        counts.put(STATUS_WAITING, countStatuses(candidates, STATUS_GROUPS.get(STATUS_WAITING)));
        counts.put(STATUS_IN_PROGRESS, countStatuses(candidates, STATUS_GROUPS.get(STATUS_IN_PROGRESS)));
        counts.put(STATUS_COMPLETED, countStatuses(candidates, STATUS_GROUPS.get(STATUS_COMPLETED)));
        counts.put(STATUS_ABANDONED, countStatuses(candidates, STATUS_GROUPS.get(STATUS_ABANDONED)));
        counts.put(STATUS_CANCELLED, countStatuses(candidates, STATUS_GROUPS.get(STATUS_CANCELLED)));
        counts.put(STATUS_TIMED_OUT, countStatuses(candidates, STATUS_GROUPS.get(STATUS_TIMED_OUT)));

        return counts.entrySet().stream()
                .map(entry -> {
                    CandidateInterviewStatusCounter counter = new CandidateInterviewStatusCounter();
                    counter.setCode(entry.getKey());
                    counter.setLabel(STATUS_LABELS.getOrDefault(entry.getKey(), entry.getKey()));
                    counter.setCount(entry.getValue());
                    return counter;
                })
                .collect(Collectors.toList());
    }

    private long countStatuses(List<CompanyJobCandidateEntity> candidates, Set<JobCandidateInterviewStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        return candidates.stream()
                .filter(candidate -> statuses.contains(candidate.getInterviewStatus()))
                .count();
    }

    private String normalizeStatus(String statusCode) {
        if (!StringUtils.hasText(statusCode)) {
            return STATUS_ALL;
        }
        String normalized = statusCode.trim().toUpperCase(Locale.ROOT);
        if (STATUS_LABELS.containsKey(normalized)) {
            return normalized;
        }
        return STATUS_ALL;
    }

    private CandidateInterviewRecordEntity ensureActiveInterviewWindow(CompanyJobCandidateEntity candidate,
                                                                       CandidateInterviewRecordEntity record,
                                                                       boolean throwIfExpired) {
        if (record == null) {
            return null;
        }
        Instant start = record.getInterviewStartedAt();
        if (start == null) {
            return record;
        }
        Instant deadline = record.getAnswerDeadlineAt();
        boolean recordChanged = false;
        if (deadline == null) {
            deadline = start.plus(ANSWER_TIME_LIMIT);
            record.setAnswerDeadlineAt(deadline);
            recordChanged = true;
        }
        Instant now = Instant.now();
        if (!now.isBefore(deadline)) {
            if (record.getInterviewEndedAt() == null) {
                record.setInterviewEndedAt(deadline);
                recordChanged = true;
            }
            if (record.getDurationSeconds() == null) {
                record.setDurationSeconds(Math.toIntExact(ChronoUnit.SECONDS.between(start, deadline)));
                recordChanged = true;
            }
            if (recordChanged) {
                record = interviewRecordRepository.save(record);
                recordChanged = false;
            }
            if (candidate.getInterviewStatus() != JobCandidateInterviewStatus.COMPLETED
                    && candidate.getInterviewStatus() != JobCandidateInterviewStatus.ABANDONED
                    && candidate.getInterviewStatus() != JobCandidateInterviewStatus.CANCELLED
                    && candidate.getInterviewStatus() != JobCandidateInterviewStatus.TIMED_OUT) {
                candidate.setInterviewStatus(JobCandidateInterviewStatus.TIMED_OUT);
                candidate.setInterviewCompletedAt(deadline);
                candidate.setUpdatedAt(now);
                candidateRepository.save(candidate);
            }
            if (throwIfExpired) {
                throw new ResponseStatusException(HttpStatus.GONE, "答题时间已结束，请重新预约面试");
            }
        } else if (recordChanged) {
            record = interviewRecordRepository.save(record);
        }
        return record;
    }

    private CandidateInterviewRecordResponse toRecordResponse(CandidateInterviewRecordEntity entity,
                                                              CompanyJobCandidateEntity candidate,
                                                              CompanyRecruitingPositionEntity position,
                                                              List<CandidateInterviewAudioEntity> audios) {
        if (entity == null) {
            return null;
        }
        CandidateInterviewRecordResponse response = new CandidateInterviewRecordResponse();
        response.setInterviewRecordId(entity.getRecordId());
        response.setJobCandidateId(entity.getJobCandidateId());
        response.setInterviewMode(entity.getInterviewMode());
        response.setInterviewerName(entity.getInterviewerName());
        response.setAiSessionId(entity.getAiSessionId());
        if (position != null) {
            response.setPositionId(position.getPositionId());
            response.setPositionName(position.getPositionName());
        }
        response.setInterviewStartedAt(entity.getInterviewStartedAt());
        response.setInterviewEndedAt(entity.getInterviewEndedAt());
        response.setAnswerDeadlineAt(entity.getAnswerDeadlineAt());
        response.setDurationSeconds(entity.getDurationSeconds());
        response.setCurrentQuestionSequence(entity.getCurrentQuestionSequence());
        response.setQuestions(readQuestionList(entity.getQuestionsJson()));
        response.setAudios(audios.stream().map(this::toAudioDto).collect(Collectors.toList()));
        response.setTranscriptRaw(entity.getTranscriptJson());
        response.setMetadata(readGenericMap(entity.getMetadataJson()));
        response.setPrecheck(toPrecheckDto(entity));
        response.setProfilePhoto(toProfilePhotoDto(entity, candidate));
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private CandidateInterviewAudioDto toAudioDto(CandidateInterviewAudioEntity entity) {
        CandidateInterviewAudioDto dto = new CandidateInterviewAudioDto();
        dto.setAudioId(entity.getAudioId());
        dto.setQuestionSequence(entity.getQuestionSequence());
        dto.setFileName(entity.getFileName());
        dto.setContentType(StringUtils.hasText(entity.getContentType()) ? entity.getContentType() : "audio/mpeg");
        dto.setDurationSeconds(entity.getDurationSeconds());
        dto.setSizeBytes(entity.getSizeBytes());
        dto.setTranscript(entity.getTranscript());
        dto.setDownloadUrl(String.format("/api/candidate/interview-sessions/%s/audios/%s", entity.getJobCandidateId(), entity.getAudioId()));
        return dto;
    }

    private CandidateInterviewPrecheckDto toPrecheckDto(CandidateInterviewRecordEntity entity) {
        if (!StringUtils.hasText(entity.getPrecheckStatus())
                && entity.getPrecheckReportJson() == null
                && entity.getPrecheckCompletedAt() == null) {
            return null;
        }
        CandidateInterviewPrecheckDto dto = new CandidateInterviewPrecheckDto();
        dto.setStatus(entity.getPrecheckStatus());
        dto.setCompletedAt(entity.getPrecheckCompletedAt());
        dto.setReport(readGenericMap(entity.getPrecheckReportJson()));
        if (entity.getPrecheckStatus() != null) {
            dto.setPassed("PASSED".equalsIgnoreCase(entity.getPrecheckStatus()));
        }
        return dto;
    }

    private CandidateInterviewProfilePhotoDto toProfilePhotoDto(CandidateInterviewRecordEntity entity,
                                                                CompanyJobCandidateEntity candidate) {
        if (entity == null || entity.getProfilePhotoData() == null) {
            return null;
        }
        CandidateInterviewProfilePhotoDto dto = new CandidateInterviewProfilePhotoDto();
        dto.setFileName(entity.getProfilePhotoFileName());
        dto.setContentType(StringUtils.hasText(entity.getProfilePhotoContentType()) ? entity.getProfilePhotoContentType() : "image/jpeg");
        dto.setSizeBytes(entity.getProfilePhotoSizeBytes() != null
                ? entity.getProfilePhotoSizeBytes()
                : (long) entity.getProfilePhotoData().length);
        dto.setUploadedAt(entity.getProfilePhotoUploadedAt());
        dto.setDownloadUrl(String.format("/api/candidate/interview-sessions/%s/profile-photo", candidate.getJobCandidateId()));
        return dto;
    }

    private List<CandidateInterviewTechnicalRequirementDto> extractRequirements(CandidateInterviewRecordEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getPrecheckReportJson())) {
            return List.of();
        }
        Map<String, Object> report = readGenericMap(entity.getPrecheckReportJson());
        Object requirements = report.get("requirements");
        if (requirements == null) {
            return List.of();
        }
        try {
            String json = objectMapper.writeValueAsString(requirements);
            return objectMapper.readValue(json, REQUIREMENT_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<CandidateInterviewQuestionDto> readQuestionList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, QUESTION_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> readGenericMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, GENERIC_MAP_TYPE);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Object value, String errorMessage) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, e);
        }
    }

    private CandidateInterviewAudioEntity resolveAudioEntity(CandidateInterviewRecordEntity record,
                                                             CandidateInterviewAnswerRequest request,
                                                             String jobCandidateId) {
        if (StringUtils.hasText(request.getAudioId())) {
            return interviewAudioRepository.findByAudioIdAndJobCandidateId(request.getAudioId(), jobCandidateId)
                    .orElseGet(() -> {
                        CandidateInterviewAudioEntity entity = new CandidateInterviewAudioEntity();
                        entity.setAudioId(request.getAudioId());
                        entity.setJobCandidateId(jobCandidateId);
                        entity.setInterviewRecordId(record.getRecordId());
                        entity.setQuestionSequence(request.getQuestionSequence());
                        return entity;
                    });
        }
        return interviewAudioRepository.findByInterviewRecordIdAndQuestionSequence(record.getRecordId(), request.getQuestionSequence())
                .orElseGet(() -> {
                    CandidateInterviewAudioEntity entity = new CandidateInterviewAudioEntity();
                    entity.setAudioId(UUID.randomUUID().toString());
                    entity.setJobCandidateId(jobCandidateId);
                    entity.setInterviewRecordId(record.getRecordId());
                    entity.setQuestionSequence(request.getQuestionSequence());
                    return entity;
                });
    }

    private String updateTranscript(String existingJson, CandidateInterviewAnswerRequest request) {
        List<Map<String, Object>> entries;
        if (!StringUtils.hasText(existingJson)) {
            entries = new ArrayList<>();
        } else {
            try {
                entries = objectMapper.readValue(existingJson, TRANSCRIPT_LIST_TYPE);
            } catch (JsonProcessingException e) {
                entries = new ArrayList<>();
            }
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("sequence", request.getQuestionSequence());
        payload.put("transcript", request.getTranscript());
        payload.put("metadata", request.getMetadata());
        payload.put("submittedAt", Instant.now().toString());
        entries.removeIf(entry -> Objects.equals(entry.get("sequence"), request.getQuestionSequence()));
        entries.add(payload);
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "序列化转写内容失败", e);
        }
    }

    private String buildAudioFileName(CompanyJobCandidateEntity candidate, Integer sequence) {
        String base = StringUtils.hasText(candidate.getCandidateName()) ? candidate.getCandidateName().replaceAll("\\s+", "_") : "answer";
        return String.format("%s_q%s.mp3", base, sequence != null ? sequence : "");
    }
}

