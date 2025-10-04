package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.JobCandidateImportResponse;
import com.example.grpcdemo.controller.dto.JobCandidateInviteRequest;
import com.example.grpcdemo.controller.dto.JobCandidateItemResponse;
import com.example.grpcdemo.controller.dto.JobCandidateListResponse;
import com.example.grpcdemo.controller.dto.JobCandidateResumeResponse;
import com.example.grpcdemo.controller.dto.JobCandidateStatusSummary;
import com.example.grpcdemo.controller.dto.JobCandidateUpdateRequest;
import com.example.grpcdemo.entity.CompanyJobCandidateEntity;
import com.example.grpcdemo.entity.CompanyRecruitingPositionEntity;
import com.example.grpcdemo.entity.InvitationTemplateEntity;
import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;
import com.example.grpcdemo.entity.JobCandidateResumeEntity;
import com.example.grpcdemo.repository.CompanyJobCandidateRepository;
import com.example.grpcdemo.repository.CompanyRecruitingPositionRepository;
import com.example.grpcdemo.repository.InvitationTemplateRepository;
import com.example.grpcdemo.repository.JobCandidateResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 企业岗位候选人相关业务逻辑。
 */
@Service
public class CompanyJobCandidateService {

    private static final Logger log = LoggerFactory.getLogger(CompanyJobCandidateService.class);

    private final CompanyRecruitingPositionRepository positionRepository;
    private final CompanyJobCandidateRepository candidateRepository;
    private final JobCandidateResumeRepository resumeRepository;
    private final InvitationTemplateRepository invitationTemplateRepository;
    private final ResumeParser resumeParser;
    private final JavaMailSender mailSender;

    public CompanyJobCandidateService(CompanyRecruitingPositionRepository positionRepository,
                                      CompanyJobCandidateRepository candidateRepository,
                                      JobCandidateResumeRepository resumeRepository,
                                      InvitationTemplateRepository invitationTemplateRepository,
                                      ResumeParser resumeParser,
                                      JavaMailSender mailSender) {
        this.positionRepository = positionRepository;
        this.candidateRepository = candidateRepository;
        this.resumeRepository = resumeRepository;
        this.invitationTemplateRepository = invitationTemplateRepository;
        this.resumeParser = resumeParser;
        this.mailSender = mailSender;
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
    public JobCandidateListResponse listCandidates(String positionId, String keyword) {
        requirePosition(positionId);
        List<CompanyJobCandidateEntity> entities;
        if (StringUtils.hasText(keyword)) {
            entities = candidateRepository.searchByKeyword(positionId, keyword.trim());
        } else {
            entities = candidateRepository.findByPositionIdOrderByCreatedAtDesc(positionId);
        }
        List<JobCandidateItemResponse> items = entities.stream()
                .map(this::toItemResponse)
                .toList();
        JobCandidateListResponse response = new JobCandidateListResponse();
        response.setCandidates(items);
        response.setSummary(buildSummary(positionId));
        return response;
    }

    @Transactional(readOnly = true)
    public JobCandidateResumeResponse getResume(String jobCandidateId) {
        CompanyJobCandidateEntity candidate = candidateRepository.findById(jobCandidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "候选人不存在"));
        JobCandidateResumeResponse response = new JobCandidateResumeResponse();
        response.setJobCandidateId(candidate.getJobCandidateId());
        response.setPositionId(candidate.getPositionId());
        response.setName(candidate.getCandidateName());
        response.setEmail(candidate.getCandidateEmail());
        response.setPhone(candidate.getCandidatePhone());
        response.setInviteStatus(candidate.getInviteStatus());
        response.setInterviewStatus(candidate.getInterviewStatus());

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
            candidate.setUpdatedAt(Instant.now());
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

    private JobCandidateStatusSummary buildSummary(String positionId) {
        JobCandidateStatusSummary summary = new JobCandidateStatusSummary();
        summary.setInvitePending(candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.INVITE_PENDING));
        summary.setInviteSent(candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.INVITE_SENT));
        summary.setInviteFailed(candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.INVITE_FAILED));
        summary.setEmailMissing(candidateRepository.countByPositionIdAndInviteStatus(positionId, JobCandidateInviteStatus.EMAIL_MISSING));
        summary.setInterviewNotStarted(candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.NOT_INTERVIEWED));
        summary.setInterviewScheduled(candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.SCHEDULED));
        summary.setInterviewInProgress(candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.IN_PROGRESS));
        summary.setInterviewCompleted(candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.COMPLETED));
        summary.setInterviewCancelled(candidateRepository.countByPositionIdAndInterviewStatus(positionId, JobCandidateInterviewStatus.CANCELLED));
        return summary;
    }

    private JobCandidateItemResponse toItemResponse(CompanyJobCandidateEntity entity) {
        boolean emailMissing = entity.getInviteStatus() == JobCandidateInviteStatus.EMAIL_MISSING
                || !StringUtils.hasText(entity.getCandidateEmail());
        boolean resumeAvailable = StringUtils.hasText(entity.getResumeId());
        return new JobCandidateItemResponse(entity.getJobCandidateId(),
                entity.getPositionId(),
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                entity.getCandidatePhone(),
                entity.getInviteStatus(),
                entity.getInterviewStatus(),
                emailMissing,
                resumeAvailable,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
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
}
