package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.CandidateAiEvaluationRequest;
import com.example.grpcdemo.controller.dto.CandidateAiEvaluationResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewAudioDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewAudioRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewQuestionDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewRecordRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewRecordResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewPrecheckDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewProfilePhotoDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewProfilePhotoRequest;
import com.example.grpcdemo.controller.dto.JobCandidateImportResponse;
import com.example.grpcdemo.controller.dto.JobCandidateInviteRequest;
import com.example.grpcdemo.controller.dto.JobCandidateItemResponse;
import com.example.grpcdemo.controller.dto.JobCandidateListResponse;
import com.example.grpcdemo.controller.dto.JobCandidateListStatus;
import com.example.grpcdemo.controller.dto.JobCandidateResumeResponse;
import com.example.grpcdemo.controller.dto.JobCandidateStatusSummary;
import com.example.grpcdemo.controller.dto.JobCandidateUpdateRequest;
import com.example.grpcdemo.entity.CompanyJobCandidateEntity;
import com.example.grpcdemo.entity.CompanyRecruitingPositionEntity;
import com.example.grpcdemo.entity.InvitationTemplateEntity;
import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;
import com.example.grpcdemo.entity.JobCandidateResumeEntity;
import com.example.grpcdemo.entity.CandidateAiEvaluationEntity;
import com.example.grpcdemo.entity.CandidateInterviewAudioEntity;
import com.example.grpcdemo.entity.CandidateInterviewRecordEntity;
import com.example.grpcdemo.repository.CompanyJobCandidateRepository;
import com.example.grpcdemo.repository.CompanyRecruitingPositionRepository;
import com.example.grpcdemo.repository.InvitationTemplateRepository;
import com.example.grpcdemo.repository.JobCandidateResumeRepository;
import com.example.grpcdemo.repository.CandidateAiEvaluationRepository;
import com.example.grpcdemo.repository.CandidateInterviewAudioRepository;
import com.example.grpcdemo.repository.CandidateInterviewRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 企业岗位候选人相关业务逻辑。
 */
@Service
public class CompanyJobCandidateService {

    private static final Logger log = LoggerFactory.getLogger(CompanyJobCandidateService.class);

    private static final TypeReference<List<CandidateInterviewQuestionDto>> QUESTION_LIST_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<String>> STRING_LIST_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<Map<String, BigDecimal>> COMPETENCY_SCORE_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<Map<String, Object>> GENERIC_MAP_TYPE =
            new TypeReference<>() {
            };

    private final CompanyRecruitingPositionRepository positionRepository;
    private final CompanyJobCandidateRepository candidateRepository;
    private final JobCandidateResumeRepository resumeRepository;
    private final InvitationTemplateRepository invitationTemplateRepository;
    private final CandidateInterviewRecordRepository interviewRecordRepository;
    private final CandidateInterviewAudioRepository interviewAudioRepository;
    private final CandidateAiEvaluationRepository aiEvaluationRepository;
    private final ResumeParser resumeParser;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    public CompanyJobCandidateService(CompanyRecruitingPositionRepository positionRepository,
                                      CompanyJobCandidateRepository candidateRepository,
                                      JobCandidateResumeRepository resumeRepository,
                                      InvitationTemplateRepository invitationTemplateRepository,
                                      CandidateInterviewRecordRepository interviewRecordRepository,
                                      CandidateInterviewAudioRepository interviewAudioRepository,
                                      CandidateAiEvaluationRepository aiEvaluationRepository,
                                      ResumeParser resumeParser,
                                      JavaMailSender mailSender,
                                      ObjectMapper objectMapper) {
        this.positionRepository = positionRepository;
        this.candidateRepository = candidateRepository;
        this.resumeRepository = resumeRepository;
        this.invitationTemplateRepository = invitationTemplateRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewAudioRepository = interviewAudioRepository;
        this.aiEvaluationRepository = aiEvaluationRepository;
        this.resumeParser = resumeParser;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JobCandidateImportResponse importCandidates(String positionId,
                                                       String uploaderUserId,
                                                       List<MultipartFile> files) {
        if (!StringUtils.hasText(positionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少岗位 ID");
        }
        if (!StringUtils.hasText(uploaderUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少上传者信息");
        }
        if (CollectionUtils.isEmpty(files)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少上传一份简历");
        }
        CompanyRecruitingPositionEntity position = requirePosition(positionId);
        List<JobCandidateItemResponse> imported = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            byte[] content;
            try {
                content = file.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取上传文件失败", e);
            }
            ResumeParsingResult parsingResult = null;
            String error = null;
            try {
                parsingResult = resumeParser.parse(new ResumeParsingCommand(content,
                        fileName,
                        contentType,
                        position.getCompanyId(),
                        positionId,
                        uploaderUserId));
            } catch (ResumeParsingException e) {
                error = e.getMessage();
                log.warn("解析简历 {} 失败: {}", fileName, e.getMessage());
            }

            CompanyJobCandidateEntity candidate = new CompanyJobCandidateEntity();
            String candidateId = UUID.randomUUID().toString();
            candidate.setJobCandidateId(candidateId);
            candidate.setPositionId(positionId);
            candidate.setUploaderUserId(uploaderUserId);

            String parsedName = parsingResult != null ? parsingResult.getName() : null;
            if (!StringUtils.hasText(parsedName)) {
                parsedName = deriveNameFromFile(fileName);
            }
            candidate.setCandidateName(parsedName);
            candidate.setCandidateEmail(parsingResult != null ? normalize(parsingResult.getEmail()) : null);
            candidate.setCandidatePhone(parsingResult != null ? normalize(parsingResult.getPhone()) : null);
            if (!StringUtils.hasText(candidate.getCandidateEmail())) {
                candidate.setInviteStatus(JobCandidateInviteStatus.EMAIL_MISSING);
            } else {
                candidate.setInviteStatus(JobCandidateInviteStatus.INVITE_PENDING);
            }
            candidate.setInterviewStatus(JobCandidateInterviewStatus.NOT_INTERVIEWED);

            CompanyJobCandidateEntity savedCandidate = candidateRepository.save(candidate);

            JobCandidateResumeEntity resume = new JobCandidateResumeEntity();
            String resumeId = UUID.randomUUID().toString();
            resume.setResumeId(resumeId);
            resume.setJobCandidateId(candidateId);
            resume.setFileName(fileName);
            resume.setFileType(contentType);
            resume.setFileContent(content);
            if (parsingResult != null) {
                resume.setParsedName(parsingResult.getName());
                resume.setParsedEmail(parsingResult.getEmail());
                resume.setParsedPhone(parsingResult.getPhone());
                resume.setParsedHtml(parsingResult.getHtmlContent());
                resume.setConfidence(parsingResult.getConfidence());
                resume.setAiRawResult(parsingResult.getRawJson());
            } else {
                resume.setAiRawResult(error);
            }
            resumeRepository.save(resume);

            savedCandidate.setResumeId(resumeId);
            candidateRepository.save(savedCandidate);

            imported.add(toItemResponse(savedCandidate));
        }
        JobCandidateImportResponse response = new JobCandidateImportResponse();
        response.setImportedCandidates(imported);
        response.setSummary(buildSummary(positionId));
        return response;
    }

    @Transactional(readOnly = true)
    public JobCandidateListResponse listCandidates(String positionId,
                                                   String keyword,
                                                   JobCandidateListStatus status,
                                                   int page,
                                                   int pageSize) {
        requirePosition(positionId);
        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), 200);
        PageRequest pageable = PageRequest.of(safePage, safePageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        StatusFilters filters = resolveStatusFilters(status);
        List<JobCandidateInviteStatus> normalizedInviteStatuses = normalizeFilters(filters.inviteStatuses());
        List<JobCandidateInterviewStatus> normalizedInterviewStatuses = normalizeFilters(filters.interviewStatuses());

        Page<CompanyJobCandidateEntity> pageResult = candidateRepository.searchByFilters(positionId,
                normalizedKeyword,
                normalizedInviteStatuses,
                normalizedInterviewStatuses,
                pageable);
        List<JobCandidateItemResponse> items = pageResult.getContent().stream()
                .map(this::toItemResponse)
                .toList();
        JobCandidateListResponse response = new JobCandidateListResponse();
        response.setCandidates(items);
        response.setSummary(buildSummary(positionId));
        response.setPage(pageResult.getNumber());
        response.setPageSize(pageResult.getSize());
        response.setTotal(pageResult.getTotalElements());
        response.setHasMore(pageResult.hasNext());
        if (pageResult.hasNext()) {
            response.setNextPage(pageResult.getNumber() + 1);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public JobCandidateResumeResponse getResume(String jobCandidateId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        candidate = refreshInterviewTimeoutIfNeeded(candidate);
        CompanyRecruitingPositionEntity position = requirePosition(candidate.getPositionId());
        JobCandidateResumeResponse response = new JobCandidateResumeResponse();
        response.setJobCandidateId(candidate.getJobCandidateId());
        response.setPositionId(candidate.getPositionId());
        response.setPositionName(position.getPositionName());
        response.setName(candidate.getCandidateName());
        response.setEmail(candidate.getCandidateEmail());
        response.setPhone(candidate.getCandidatePhone());
        response.setInviteStatus(candidate.getInviteStatus());
        response.setInterviewStatus(candidate.getInterviewStatus());
        response.setInterviewRecordAvailable(StringUtils.hasText(candidate.getInterviewRecordId()));
        response.setAiEvaluationAvailable(StringUtils.hasText(candidate.getAiEvaluationId()));
        response.setInterviewCompletedAt(candidate.getInterviewCompletedAt());
        response.setInterviewDeadlineAt(candidate.getInterviewDeadlineAt());

        if (StringUtils.hasText(candidate.getResumeId())) {
            Optional<JobCandidateResumeEntity> resumeOpt = resumeRepository.findById(candidate.getResumeId());
            resumeOpt.ifPresent(resume -> {
                response.setFileName(resume.getFileName());
                response.setFileType(resume.getFileType());
                response.setResumeHtml(resume.getParsedHtml());
                response.setResumeDocumentHtml(buildDocumentHtml(resume.getFileContent(), resume.getFileType()));
                response.setUploadedAt(resume.getCreatedAt());
                response.setConfidence(resume.getConfidence());
            });
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ResumeFilePayload getResumeFile(String jobCandidateId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        if (!StringUtils.hasText(candidate.getResumeId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人缺少简历文件");
        }
        JobCandidateResumeEntity resume = resumeRepository.findById(candidate.getResumeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人简历不存在"));
        byte[] content = resume.getFileContent();
        if (content == null || content.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人简历文件不存在");
        }
        String fileType = StringUtils.hasText(resume.getFileType()) ? resume.getFileType() : "application/pdf";
        String fileName = resolveResumeFileName(candidate, resume);
        return new ResumeFilePayload(fileName, fileType, content);
    }

    @Transactional(readOnly = true)
    public InterviewAudioPayload getInterviewAudio(String jobCandidateId, String audioId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        CandidateInterviewAudioEntity audio = interviewAudioRepository.findByAudioIdAndJobCandidateId(audioId, jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "音频不存在"));
        byte[] data = audio.getAudioData();
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "音频内容不存在");
        }
        String contentType = StringUtils.hasText(audio.getContentType()) ? audio.getContentType() : "audio/mpeg";
        String fileName = resolveAudioFileName(candidate, audio);
        return new InterviewAudioPayload(fileName, contentType, data);
    }

    @Transactional(readOnly = true)
    public InterviewProfilePhotoPayload getInterviewProfilePhoto(String jobCandidateId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
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
        String fileName = record.getProfilePhotoFileName();
        if (!StringUtils.hasText(fileName)) {
            fileName = (StringUtils.hasText(candidate.getCandidateName())
                    ? candidate.getCandidateName().replaceAll("\\s+", "_")
                    : "profile") + ".jpg";
        }
        return new InterviewProfilePhotoPayload(fileName, contentType, data);
    }

    @Transactional
    public JobCandidateItemResponse updateCandidate(String jobCandidateId, JobCandidateUpdateRequest request) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        boolean changed = false;
        if (request.getName() != null) {
            candidate.setCandidateName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
            changed = true;
        }
        if (request.getEmail() != null) {
            String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null;
            candidate.setCandidateEmail(email);
            if (!StringUtils.hasText(email)) {
                candidate.setInviteStatus(JobCandidateInviteStatus.EMAIL_MISSING);
            } else if (candidate.getInviteStatus() == JobCandidateInviteStatus.EMAIL_MISSING) {
                candidate.setInviteStatus(JobCandidateInviteStatus.INVITE_PENDING);
            }
            changed = true;
        }
        if (request.getPhone() != null) {
            candidate.setCandidatePhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
            changed = true;
        }
        if (request.getInviteStatus() != null) {
            candidate.setInviteStatus(request.getInviteStatus());
            changed = true;
        }
        if (request.getInterviewStatus() != null) {
            candidate.setInterviewStatus(request.getInterviewStatus());
            if (request.getInterviewStatus() == JobCandidateInterviewStatus.ABANDONED) {
                candidate.setCandidateResponseAt(Instant.now());
            } else if (request.getInterviewStatus() == JobCandidateInterviewStatus.COMPLETED
                    && candidate.getInterviewCompletedAt() == null) {
                candidate.setInterviewCompletedAt(Instant.now());
            } else if (request.getInterviewStatus() == JobCandidateInterviewStatus.TIMED_OUT
                    && candidate.getInterviewDeadlineAt() == null) {
                candidate.setInterviewDeadlineAt(Instant.now());
            }
            changed = true;
        }
        if (changed) {
            candidate.setUpdatedAt(Instant.now());
        }
        candidateRepository.save(candidate);

        if (request.getResumeHtml() != null && StringUtils.hasText(candidate.getResumeId())) {
            resumeRepository.findById(candidate.getResumeId()).ifPresent(resume -> {
                resume.setParsedHtml(request.getResumeHtml());
                if (request.getName() != null) {
                    resume.setParsedName(candidate.getCandidateName());
                }
                if (request.getEmail() != null) {
                    resume.setParsedEmail(candidate.getCandidateEmail());
                }
                if (request.getPhone() != null) {
                    resume.setParsedPhone(candidate.getCandidatePhone());
                }
                resumeRepository.save(resume);
            });
        }
        return toItemResponse(candidate);
    }

    @Transactional
    public JobCandidateItemResponse sendInvite(String jobCandidateId, JobCandidateInviteRequest request) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        if (!StringUtils.hasText(candidate.getCandidateEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "候选人缺少邮箱，无法发送邀约");
        }
        CompanyRecruitingPositionEntity position = requirePosition(candidate.getPositionId());
        InvitationTemplateEntity template = resolveTemplate(position.getCompanyId(), request);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(candidate.getCandidateEmail());
            message.setSubject(template.getSubject());
            message.setText(template.getBody());
            if (request != null && !CollectionUtils.isEmpty(request.getCc())) {
                message.setCc(request.getCc().toArray(new String[0]));
            }
            mailSender.send(message);
            candidate.setInviteStatus(JobCandidateInviteStatus.INVITE_SENT);
            Instant now = Instant.now();
            candidate.setLastInviteSentAt(now);
            candidate.setInterviewDeadlineAt(now.plus(15, ChronoUnit.DAYS));
            candidate.setCandidateResponseAt(null);
            candidate.setUpdatedAt(now);
            candidateRepository.save(candidate);
            return toItemResponse(candidate);
        } catch (MailException e) {
            candidate.setInviteStatus(JobCandidateInviteStatus.INVITE_FAILED);
            candidate.setUpdatedAt(Instant.now());
            candidateRepository.save(candidate);
            log.error("发送邀约邮件失败: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "发送邀约邮件失败", e);
        }
    }

    @Transactional(readOnly = true)
    public CandidateInterviewRecordResponse getInterviewRecord(String jobCandidateId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        candidate = refreshInterviewTimeoutIfNeeded(candidate);
        CompanyRecruitingPositionEntity position = requirePosition(candidate.getPositionId());
        CandidateInterviewRecordEntity record = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到面试记录"));
        List<CandidateInterviewAudioEntity> audios = interviewAudioRepository
                .findByJobCandidateIdAndInterviewRecordIdOrderByQuestionSequenceAscCreatedAtAsc(jobCandidateId,
                        record.getRecordId());
        return toInterviewRecordResponse(record, candidate, position, audios);
    }

    @Transactional
    public CandidateInterviewRecordResponse upsertInterviewRecord(String jobCandidateId,
                                                                  CandidateInterviewRecordRequest request) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        CandidateInterviewRecordEntity entity = interviewRecordRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElse(null);
        if (entity == null) {
            entity = new CandidateInterviewRecordEntity();
            entity.setRecordId(resolveRecordId(request != null ? request.getInterviewRecordId() : null));
            entity.setJobCandidateId(jobCandidateId);
        } else if (request != null && StringUtils.hasText(request.getInterviewRecordId())
                && !request.getInterviewRecordId().equals(entity.getRecordId())) {
            entity.setRecordId(request.getInterviewRecordId());
        }
        if (request != null) {
            entity.setAiSessionId(request.getAiSessionId());
            entity.setInterviewMode(request.getInterviewMode());
            entity.setInterviewerName(request.getInterviewerName());
            entity.setInterviewStartedAt(request.getInterviewStartedAt());
            entity.setInterviewEndedAt(request.getInterviewEndedAt());
            Integer duration = request.getDurationSeconds();
            if (duration == null && request.getInterviewStartedAt() != null && request.getInterviewEndedAt() != null) {
                duration = Math.toIntExact(ChronoUnit.SECONDS.between(request.getInterviewStartedAt(),
                        request.getInterviewEndedAt()));
            }
            entity.setDurationSeconds(duration);
            entity.setQuestionsJson(writeJson(request.getQuestions(), "序列化面试题目失败"));
            entity.setTranscriptJson(request.getTranscriptRaw());
            entity.setMetadataJson(writeJson(request.getMetadata(), "序列化面试元数据失败"));
            entity.setCurrentQuestionSequence(request.getCurrentQuestionSequence());
            applyPrecheck(entity, request.getPrecheck());
            applyProfilePhoto(entity, request.getProfilePhoto());
        }
        CandidateInterviewRecordEntity saved = interviewRecordRepository.save(entity);

        List<CandidateInterviewAudioEntity> audios;
        if (request != null && request.getAudios() != null) {
            audios = replaceInterviewAudios(candidate, saved, request.getAudios());
        } else {
            audios = interviewAudioRepository
                    .findByJobCandidateIdAndInterviewRecordIdOrderByQuestionSequenceAscCreatedAtAsc(jobCandidateId,
                            saved.getRecordId());
        }

        candidate.setInterviewRecordId(saved.getRecordId());
        if (saved.getInterviewEndedAt() != null) {
            candidate.setInterviewCompletedAt(saved.getInterviewEndedAt());
        } else if (candidate.getInterviewCompletedAt() == null) {
            candidate.setInterviewCompletedAt(saved.getUpdatedAt());
        }
        if (candidate.getInterviewStatus() != JobCandidateInterviewStatus.ABANDONED
                && candidate.getInterviewStatus() != JobCandidateInterviewStatus.CANCELLED
                && candidate.getInterviewStatus() != JobCandidateInterviewStatus.TIMED_OUT) {
            candidate.setInterviewStatus(JobCandidateInterviewStatus.COMPLETED);
        }
        candidate.setUpdatedAt(Instant.now());
        candidateRepository.save(candidate);

        CompanyRecruitingPositionEntity position = requirePosition(candidate.getPositionId());
        return toInterviewRecordResponse(saved, candidate, position, audios);
    }

    @Transactional(readOnly = true)
    public CandidateAiEvaluationResponse getAiEvaluation(String jobCandidateId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        candidate = refreshInterviewTimeoutIfNeeded(candidate);
        CompanyRecruitingPositionEntity position = requirePosition(candidate.getPositionId());
        CandidateAiEvaluationEntity evaluation = aiEvaluationRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到 AI 评估报告"));
        CandidateAiEvaluationResponse response = toAiEvaluationResponse(evaluation);
        response.setPositionId(position.getPositionId());
        response.setPositionName(position.getPositionName());
        return response;
    }

    @Transactional
    public CandidateAiEvaluationResponse upsertAiEvaluation(String jobCandidateId,
                                                            CandidateAiEvaluationRequest request) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        CandidateAiEvaluationEntity entity = aiEvaluationRepository
                .findFirstByJobCandidateIdOrderByCreatedAtDesc(jobCandidateId)
                .orElse(null);
        if (entity == null) {
            entity = new CandidateAiEvaluationEntity();
            entity.setEvaluationId(resolveEvaluationId(request != null ? request.getEvaluationId() : null));
            entity.setJobCandidateId(jobCandidateId);
        } else if (request != null && StringUtils.hasText(request.getEvaluationId())
                && !request.getEvaluationId().equals(entity.getEvaluationId())) {
            entity.setEvaluationId(request.getEvaluationId());
        }
        if (request != null) {
            entity.setInterviewRecordId(resolveInterviewRecordId(candidate, request.getInterviewRecordId()));
            entity.setOverallScore(request.getOverallScore());
            entity.setScoreLevel(request.getScoreLevel());
            entity.setStrengthsJson(writeJson(request.getStrengths(), "序列化优点失败"));
            entity.setWeaknessesJson(writeJson(request.getWeaknesses(), "序列化待改进项失败"));
            entity.setRiskAlertsJson(writeJson(request.getRiskAlerts(), "序列化风险提示失败"));
            entity.setRecommendationsJson(writeJson(request.getRecommendations(), "序列化建议失败"));
            entity.setCompetencyScoresJson(writeJson(request.getCompetencyScores(), "序列化能力评分失败"));
            entity.setCustomMetricsJson(writeJson(request.getCustomMetrics(), "序列化扩展指标失败"));
            entity.setAiModelVersion(request.getAiModelVersion());
            entity.setEvaluatedAt(request.getEvaluatedAt() != null ? request.getEvaluatedAt() : Instant.now());
            entity.setRawPayload(request.getRawPayload());
        }
        CandidateAiEvaluationEntity saved = aiEvaluationRepository.save(entity);

        candidate.setAiEvaluationId(saved.getEvaluationId());
        if (saved.getEvaluatedAt() != null) {
            candidate.setInterviewCompletedAt(saved.getEvaluatedAt());
        } else if (candidate.getInterviewCompletedAt() == null) {
            candidate.setInterviewCompletedAt(saved.getUpdatedAt());
        }
        if (candidate.getInterviewStatus() != JobCandidateInterviewStatus.ABANDONED
                && candidate.getInterviewStatus() != JobCandidateInterviewStatus.CANCELLED
                && candidate.getInterviewStatus() != JobCandidateInterviewStatus.TIMED_OUT) {
            candidate.setInterviewStatus(JobCandidateInterviewStatus.COMPLETED);
        }
        candidate.setUpdatedAt(Instant.now());
        candidateRepository.save(candidate);

        CompanyRecruitingPositionEntity position = requirePosition(candidate.getPositionId());
        CandidateAiEvaluationResponse response = toAiEvaluationResponse(saved);
        response.setPositionId(position.getPositionId());
        response.setPositionName(position.getPositionName());
        return response;
    }

    private CandidateInterviewRecordResponse toInterviewRecordResponse(CandidateInterviewRecordEntity entity,
                                                                       CompanyJobCandidateEntity candidate,
                                                                       CompanyRecruitingPositionEntity position,
                                                                       List<CandidateInterviewAudioEntity> audios) {
        CandidateInterviewRecordResponse response = new CandidateInterviewRecordResponse();
        response.setInterviewRecordId(entity.getRecordId());
        response.setJobCandidateId(entity.getJobCandidateId());
        response.setInterviewMode(entity.getInterviewMode());
        response.setInterviewerName(entity.getInterviewerName());
        response.setAiSessionId(entity.getAiSessionId());
        response.setPositionId(position.getPositionId());
        response.setPositionName(position.getPositionName());
        response.setInterviewStartedAt(entity.getInterviewStartedAt());
        response.setInterviewEndedAt(entity.getInterviewEndedAt());
        response.setAnswerDeadlineAt(entity.getAnswerDeadlineAt());
        response.setDurationSeconds(entity.getDurationSeconds());
        response.setQuestions(readQuestionList(entity.getQuestionsJson()));
        response.setAudios(audios.stream()
                .map(this::toAudioDto)
                .toList());
        response.setTranscriptRaw(entity.getTranscriptJson());
        response.setMetadata(readGenericMap(entity.getMetadataJson()));
        response.setCurrentQuestionSequence(entity.getCurrentQuestionSequence());
        response.setPrecheck(toPrecheckDto(entity));
        response.setProfilePhoto(toProfilePhotoDto(entity));
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setTimedOut(candidate.getInterviewStatus() == JobCandidateInterviewStatus.TIMED_OUT);
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
        dto.setDownloadUrl(String.format("/api/enterprise/job-candidates/%s/interview-record/audios/%s",
                entity.getJobCandidateId(), entity.getAudioId()));
        return dto;
    }

    private void applyPrecheck(CandidateInterviewRecordEntity entity, CandidateInterviewPrecheckDto precheck) {
        if (precheck == null) {
            return;
        }
        if (StringUtils.hasText(precheck.getStatus())) {
            entity.setPrecheckStatus(precheck.getStatus());
        } else if (precheck.getPassed() != null) {
            entity.setPrecheckStatus(precheck.getPassed() ? "PASSED" : "FAILED");
        }
        if (precheck.getCompletedAt() != null) {
            entity.setPrecheckCompletedAt(precheck.getCompletedAt());
        }
        entity.setPrecheckReportJson(writeJson(precheck.getReport(), "序列化预检报告失败"));
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

    private void applyProfilePhoto(CandidateInterviewRecordEntity entity, CandidateInterviewProfilePhotoRequest request) {
        if (request == null) {
            return;
        }
        if (Boolean.TRUE.equals(request.getRemove())) {
            entity.setProfilePhotoData(null);
            entity.setProfilePhotoFileName(null);
            entity.setProfilePhotoContentType(null);
            entity.setProfilePhotoSizeBytes(null);
            entity.setProfilePhotoUploadedAt(null);
            return;
        }
        if (!StringUtils.hasText(request.getBase64Content())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像内容不能为空");
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(request.getBase64Content());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像内容不是有效的 Base64 编码", e);
        }
        entity.setProfilePhotoData(data);
        entity.setProfilePhotoFileName(request.getFileName());
        entity.setProfilePhotoContentType(StringUtils.hasText(request.getContentType())
                ? request.getContentType()
                : "image/jpeg");
        long size = data.length;
        if (request.getSizeBytes() != null) {
            size = request.getSizeBytes();
        }
        entity.setProfilePhotoSizeBytes(size);
        entity.setProfilePhotoUploadedAt(Instant.now());
    }

    private CandidateInterviewProfilePhotoDto toProfilePhotoDto(CandidateInterviewRecordEntity entity) {
        if (entity.getProfilePhotoData() == null) {
            return null;
        }
        CandidateInterviewProfilePhotoDto dto = new CandidateInterviewProfilePhotoDto();
        dto.setFileName(entity.getProfilePhotoFileName());
        dto.setContentType(StringUtils.hasText(entity.getProfilePhotoContentType())
                ? entity.getProfilePhotoContentType()
                : "image/jpeg");
        dto.setSizeBytes(entity.getProfilePhotoSizeBytes() != null
                ? entity.getProfilePhotoSizeBytes()
                : (long) entity.getProfilePhotoData().length);
        dto.setUploadedAt(entity.getProfilePhotoUploadedAt());
        if (StringUtils.hasText(entity.getJobCandidateId()) && StringUtils.hasText(entity.getRecordId())) {
            dto.setDownloadUrl(String.format("/api/enterprise/job-candidates/%s/interview-record/profile-photo", entity.getJobCandidateId()));
        }
        return dto;
    }

    private List<CandidateInterviewAudioEntity> replaceInterviewAudios(CompanyJobCandidateEntity candidate,
                                                                       CandidateInterviewRecordEntity record,
                                                                       List<CandidateInterviewAudioRequest> requests) {
        interviewAudioRepository.deleteByInterviewRecordId(record.getRecordId());
        if (CollectionUtils.isEmpty(requests)) {
            return Collections.emptyList();
        }
        List<CandidateInterviewAudioEntity> saved = new ArrayList<>();
        for (CandidateInterviewAudioRequest audioRequest : requests) {
            if (audioRequest == null) {
                continue;
            }
            byte[] data;
            try {
                data = Base64.getDecoder().decode(audioRequest.getBase64Content());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "音频内容不是有效的 Base64 编码", e);
            }
            if (data == null || data.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "音频内容不能为空");
            }
            CandidateInterviewAudioEntity audio = new CandidateInterviewAudioEntity();
            audio.setAudioId(StringUtils.hasText(audioRequest.getAudioId())
                    ? audioRequest.getAudioId()
                    : UUID.randomUUID().toString());
            audio.setJobCandidateId(candidate.getJobCandidateId());
            audio.setInterviewRecordId(record.getRecordId());
            audio.setQuestionSequence(audioRequest.getQuestionSequence());
            audio.setFileName(audioRequest.getFileName());
            audio.setContentType(audioRequest.getContentType());
            audio.setDurationSeconds(audioRequest.getDurationSeconds());
            audio.setTranscript(audioRequest.getTranscript());
            audio.setAudioData(data);
            audio.setSizeBytes((long) data.length);
            saved.add(interviewAudioRepository.save(audio));
        }
        saved.sort((a, b) -> {
            Integer seqA = a.getQuestionSequence();
            Integer seqB = b.getQuestionSequence();
            if (seqA == null && seqB == null) {
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            }
            if (seqA == null) {
                return 1;
            }
            if (seqB == null) {
                return -1;
            }
            int compare = Integer.compare(seqA, seqB);
            if (compare != 0) {
                return compare;
            }
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        return saved;
    }

    private CandidateAiEvaluationResponse toAiEvaluationResponse(CandidateAiEvaluationEntity entity) {
        CandidateAiEvaluationResponse response = new CandidateAiEvaluationResponse();
        response.setEvaluationId(entity.getEvaluationId());
        response.setJobCandidateId(entity.getJobCandidateId());
        response.setInterviewRecordId(entity.getInterviewRecordId());
        response.setOverallScore(entity.getOverallScore());
        response.setScoreLevel(entity.getScoreLevel());
        response.setStrengths(readStringList(entity.getStrengthsJson()));
        response.setWeaknesses(readStringList(entity.getWeaknessesJson()));
        response.setRiskAlerts(readStringList(entity.getRiskAlertsJson()));
        response.setRecommendations(readStringList(entity.getRecommendationsJson()));
        response.setCompetencyScores(readCompetencyScores(entity.getCompetencyScoresJson()));
        response.setCustomMetrics(readGenericMap(entity.getCustomMetricsJson()));
        response.setAiModelVersion(entity.getAiModelVersion());
        response.setEvaluatedAt(entity.getEvaluatedAt());
        response.setRawPayload(entity.getRawPayload());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private String resolveRecordId(String requestedId) {
        if (StringUtils.hasText(requestedId)) {
            return requestedId;
        }
        return UUID.randomUUID().toString();
    }

    private String resolveEvaluationId(String requestedId) {
        if (StringUtils.hasText(requestedId)) {
            return requestedId;
        }
        return UUID.randomUUID().toString();
    }

    private String resolveInterviewRecordId(CompanyJobCandidateEntity candidate, String requestedId) {
        if (StringUtils.hasText(requestedId)) {
            return requestedId;
        }
        return candidate.getInterviewRecordId();
    }

    private List<CandidateInterviewQuestionDto> readQuestionList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, QUESTION_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.error("反序列化面试题目失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.error("反序列化字符串列表失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Map<String, BigDecimal> readCompetencyScores(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, COMPETENCY_SCORE_TYPE);
        } catch (JsonProcessingException e) {
            log.error("反序列化能力评分失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> readGenericMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, GENERIC_MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.error("反序列化元数据失败: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private String writeJson(Object value, String errorMessage) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("{}: {}", errorMessage, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage, e);
        }
    }

    private CompanyJobCandidateEntity refreshInterviewTimeoutIfNeeded(CompanyJobCandidateEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getInviteStatus() == JobCandidateInviteStatus.INVITE_SENT
                && (entity.getInterviewStatus() == JobCandidateInterviewStatus.NOT_INTERVIEWED
                || entity.getInterviewStatus() == JobCandidateInterviewStatus.SCHEDULED)
                && entity.getInterviewDeadlineAt() != null
                && Instant.now().isAfter(entity.getInterviewDeadlineAt())) {
            entity.setInterviewStatus(JobCandidateInterviewStatus.TIMED_OUT);
            entity.setUpdatedAt(Instant.now());
            return candidateRepository.save(entity);
        }
        return entity;
    }

    private InvitationTemplateEntity resolveTemplate(String companyId, JobCandidateInviteRequest request) {
        if (request != null && StringUtils.hasText(request.getTemplateId())) {
            InvitationTemplateEntity template = invitationTemplateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀约模版不存在"));
            if (!template.getCompanyId().equals(companyId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模版不属于当前企业");
            }
            return template;
        }
        return invitationTemplateRepository.findFirstByCompanyIdAndDefaultTemplateTrue(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "企业未配置默认邀约模版"));
    }

    private CompanyRecruitingPositionEntity requirePosition(String positionId) {
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在"));
    }

    private <T> List<T> normalizeFilters(List<T> values) {
        if (values == null) {
            return null;
        }
        List<T> filtered = values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return filtered.isEmpty() ? null : filtered;
    }

    private StatusFilters resolveStatusFilters(JobCandidateListStatus status) {
        JobCandidateListStatus effective = status != null ? status : JobCandidateListStatus.ALL;
        return switch (effective) {
            case WAITING_INVITE -> new StatusFilters(List.of(
                    JobCandidateInviteStatus.INVITE_PENDING,
                    JobCandidateInviteStatus.EMAIL_MISSING,
                    JobCandidateInviteStatus.INVITE_FAILED
            ), null);
            case NOT_INTERVIEWED -> new StatusFilters(null, List.of(
                    JobCandidateInterviewStatus.NOT_INTERVIEWED,
                    JobCandidateInterviewStatus.SCHEDULED,
                    JobCandidateInterviewStatus.IN_PROGRESS
            ));
            case INTERVIEWED -> new StatusFilters(null, List.of(JobCandidateInterviewStatus.COMPLETED));
            case DROPPED -> new StatusFilters(null, List.of(
                    JobCandidateInterviewStatus.CANCELLED,
                    JobCandidateInterviewStatus.ABANDONED,
                    JobCandidateInterviewStatus.TIMED_OUT
            ));
            case ALL -> new StatusFilters(null, null);
        };
    }

    private JobCandidateStatusSummary buildSummary(String positionId) {
        JobCandidateStatusSummary summary = new JobCandidateStatusSummary();
        long waitingInvite = candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.INVITE_PENDING)
                + candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.EMAIL_MISSING)
                + candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.INVITE_FAILED);
        summary.setWaitingInvite(waitingInvite);

        long invited = candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.INVITE_SENT);
        summary.setInvited(invited);

        long notInterviewed = candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.NOT_INTERVIEWED)
                + candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.SCHEDULED)
                + candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.IN_PROGRESS);
        summary.setNotInterviewed(notInterviewed);

        long interviewed = candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.COMPLETED);
        summary.setInterviewed(interviewed);

        long dropped = candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.CANCELLED)
                + candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.ABANDONED)
                + candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.TIMED_OUT);
        summary.setDropped(dropped);

        long timedOut = candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.TIMED_OUT);
        summary.setTimedOut(timedOut);

        long all = candidateRepository.countByPositionId(positionId);
        summary.setAll(all);

        EnumMap<JobCandidateListStatus, Long> counts = new EnumMap<>(JobCandidateListStatus.class);
        counts.put(JobCandidateListStatus.WAITING_INVITE, waitingInvite);
        counts.put(JobCandidateListStatus.NOT_INTERVIEWED, notInterviewed);
        counts.put(JobCandidateListStatus.INTERVIEWED, interviewed);
        counts.put(JobCandidateListStatus.DROPPED, dropped);
        counts.put(JobCandidateListStatus.ALL, all);
        summary.setStatusCounts(counts);
        return summary;
    }

    private static final class StatusFilters {
        private final List<JobCandidateInviteStatus> inviteStatuses;
        private final List<JobCandidateInterviewStatus> interviewStatuses;

        private StatusFilters(List<JobCandidateInviteStatus> inviteStatuses,
                              List<JobCandidateInterviewStatus> interviewStatuses) {
            this.inviteStatuses = inviteStatuses;
            this.interviewStatuses = interviewStatuses;
        }

        private List<JobCandidateInviteStatus> inviteStatuses() {
            return inviteStatuses;
        }

        private List<JobCandidateInterviewStatus> interviewStatuses() {
            return interviewStatuses;
        }
    }

    private JobCandidateItemResponse toItemResponse(CompanyJobCandidateEntity entity) {
        CompanyJobCandidateEntity candidate = refreshInterviewTimeoutIfNeeded(entity);
        boolean emailMissing = candidate.getInviteStatus() == JobCandidateInviteStatus.EMAIL_MISSING
                || !StringUtils.hasText(candidate.getCandidateEmail());
        boolean resumeAvailable = StringUtils.hasText(candidate.getResumeId());
        boolean interviewRecordAvailable = StringUtils.hasText(candidate.getInterviewRecordId());
        boolean aiEvaluationAvailable = StringUtils.hasText(candidate.getAiEvaluationId());
        return new JobCandidateItemResponse(candidate.getJobCandidateId(),
                candidate.getPositionId(),
                candidate.getCandidateName(),
                candidate.getCandidateEmail(),
                candidate.getCandidatePhone(),
                candidate.getInviteStatus(),
                candidate.getInterviewStatus(),
                emailMissing,
                resumeAvailable,
                interviewRecordAvailable,
                aiEvaluationAvailable,
                candidate.getInterviewCompletedAt(),
                candidate.getInterviewDeadlineAt(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt());
    }

    private String buildDocumentHtml(byte[] content, String fileType) {
        if (content == null || content.length == 0) {
            return null;
        }
        String mimeType = StringUtils.hasText(fileType) ? fileType : "application/pdf";
        String base64 = Base64.getEncoder().encodeToString(content);
        return "<iframe src=\"data:" + mimeType + ";base64," + base64
                + "\" style=\"width:100%;height:100%;border:none;\"></iframe>";
    }

    private String resolveResumeFileName(CompanyJobCandidateEntity candidate, JobCandidateResumeEntity resume) {
        if (StringUtils.hasText(resume.getFileName())) {
            return resume.getFileName();
        }
        if (StringUtils.hasText(candidate.getCandidateName())) {
            return candidate.getCandidateName().trim() + ".pdf";
        }
        return resume.getResumeId() + ".pdf";
    }

    private String resolveAudioFileName(CompanyJobCandidateEntity candidate, CandidateInterviewAudioEntity audio) {
        if (StringUtils.hasText(audio.getFileName())) {
            return audio.getFileName();
        }
        String baseName = StringUtils.hasText(candidate.getCandidateName())
                ? candidate.getCandidateName().trim()
                : "interview-audio";
        String sequenceSuffix = audio.getQuestionSequence() != null ? "-q" + audio.getQuestionSequence() : "";
        String contentType = audio.getContentType();
        String extension;
        if (StringUtils.hasText(contentType)) {
            if (contentType.contains("wav")) {
                extension = ".wav";
            } else if (contentType.contains("ogg")) {
                extension = ".ogg";
            } else if (contentType.contains("m4a")) {
                extension = ".m4a";
            } else if (contentType.contains("aac")) {
                extension = ".aac";
            } else {
                extension = ".mp3";
            }
        } else {
            extension = ".mp3";
        }
        return baseName + sequenceSuffix + extension;
    }

    private String deriveNameFromFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "未命名候选人";
        }
        String name = fileName;
        int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (slash >= 0 && slash < fileName.length() - 1) {
            name = fileName.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record ResumeFilePayload(String fileName, String fileType, byte[] content) {
    }

    public record InterviewAudioPayload(String fileName, String fileType, byte[] content) {
    }

    public record InterviewProfilePhotoPayload(String fileName, String fileType, byte[] content) {
    }
}
