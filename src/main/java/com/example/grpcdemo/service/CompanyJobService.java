package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.JobCardResponse;
import com.example.grpcdemo.controller.dto.JobDetailResponse;
import com.example.grpcdemo.controller.dto.JobDocumentResponse;
import com.example.grpcdemo.controller.dto.JobSummaryResponse;
import com.example.grpcdemo.controller.dto.JobUpdateRequest;
import com.example.grpcdemo.entity.CompanyJobDocumentEntity;
import com.example.grpcdemo.entity.CompanyRecruitingPositionEntity;
import com.example.grpcdemo.entity.RecruitingPositionSource;
import com.example.grpcdemo.entity.RecruitingPositionStatus;
import com.example.grpcdemo.repository.CompanyContactRepository;
import com.example.grpcdemo.repository.CompanyJobDocumentRepository;
import com.example.grpcdemo.repository.CompanyProfileRepository;
import com.example.grpcdemo.repository.CompanyRecruitingPositionRepository;
import com.example.grpcdemo.storage.StorageCategory;
import com.example.grpcdemo.storage.StorageException;
import com.example.grpcdemo.storage.StorageObjectPointer;
import com.example.grpcdemo.storage.SupabaseStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;

/**
 * 与企业岗位卡片交互的领域服务。
 */
@Service
public class CompanyJobService {

    private static final Logger log = LoggerFactory.getLogger(CompanyJobService.class);

    private final CompanyRecruitingPositionRepository recruitingPositionRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CompanyContactRepository companyContactRepository;
    private final CompanyJobDocumentRepository jobDocumentRepository;
    private final JobDescriptionParser jobDescriptionParser;
    private final SupabaseStorageService storageService;

    public CompanyJobService(CompanyRecruitingPositionRepository recruitingPositionRepository,
                             CompanyProfileRepository companyProfileRepository,
                             CompanyContactRepository companyContactRepository,
                             CompanyJobDocumentRepository jobDocumentRepository,
                             JobDescriptionParser jobDescriptionParser,
                             SupabaseStorageService storageService) {
        this.recruitingPositionRepository = recruitingPositionRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.companyContactRepository = companyContactRepository;
        this.jobDocumentRepository = jobDocumentRepository;
        this.jobDescriptionParser = jobDescriptionParser;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public JobSummaryResponse loadSummary(String companyId, String userId) {
        String resolvedCompanyId = resolveCompanyId(companyId, userId);
        long total = recruitingPositionRepository.countByCompanyId(resolvedCompanyId);
        boolean shouldShowOnboarding = total == 0;
        return new JobSummaryResponse(resolvedCompanyId, total, shouldShowOnboarding);
    }

    @Transactional(readOnly = true)
    public List<JobCardResponse> listCards(String companyId, String userId, String keyword) {
        String resolvedCompanyId = resolveCompanyId(companyId, userId);
        List<CompanyRecruitingPositionEntity> positions;
        if (StringUtils.hasText(keyword)) {
            positions = recruitingPositionRepository
                    .findByCompanyIdAndPositionNameContainingIgnoreCaseOrderByUpdatedAtDesc(resolvedCompanyId, keyword.trim());
        } else {
            positions = recruitingPositionRepository.findByCompanyIdOrderByUpdatedAtDesc(resolvedCompanyId);
        }
        return positions.stream()
                .map(this::toCard)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDetailResponse getDetail(String positionId) {
        CompanyRecruitingPositionEntity entity = recruitingPositionRepository.findById(positionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到岗位"));
        Optional<CompanyJobDocumentEntity> document = jobDocumentRepository.findByPositionId(positionId);
        return toDetail(entity, document.orElse(null));
    }

    @Transactional
    public JobDetailResponse importPosition(String companyId,
                                            String userId,
                                            String uploaderUserId,
                                            String fileName,
                                            String contentType,
                                            byte[] content) {
        if (content == null || content.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (!StringUtils.hasText(uploaderUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少上传者信息");
        }
        String resolvedCompanyId = resolveCompanyId(companyId, userId);
        Instant now = Instant.now();
        String positionId = UUID.randomUUID().toString();

        JobParsingResult parsingResult = null;
        String errorMessage = null;
        try {
            parsingResult = jobDescriptionParser.parse(new JobParsingCommand(content, fileName, contentType, resolvedCompanyId, uploaderUserId));
        } catch (JobParsingException e) {
            log.warn("解析岗位 JD 失败: {}", e.getMessage());
            errorMessage = e.getMessage();
        }

        String fallbackName = deriveTitleFromFileName(fileName);
        CompanyRecruitingPositionEntity entity = new CompanyRecruitingPositionEntity();
        entity.setPositionId(positionId);
        entity.setCompanyId(resolvedCompanyId);
        entity.setSource(RecruitingPositionSource.AI_IMPORT);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        if (parsingResult != null && parsingResult.getTitle() != null) {
            entity.setPositionName(parsingResult.getTitle());
        } else {
            entity.setPositionName(fallbackName);
        }
        entity.setPositionLocation(parsingResult != null ? parsingResult.getLocation() : null);
        entity.setPublisherNickname(parsingResult != null ? parsingResult.getPublisherNickname() : null);
        if (parsingResult != null && parsingResult.hasStructuredFields()) {
            entity.setStatus(RecruitingPositionStatus.DRAFT_PARSED);
        } else if (parsingResult != null) {
            entity.setStatus(RecruitingPositionStatus.DRAFT_PARSED);
        } else {
            entity.setStatus(RecruitingPositionStatus.PARSE_FAILED);
        }

        CompanyJobDocumentEntity document = new CompanyJobDocumentEntity();
        String documentId = UUID.randomUUID().toString();
        document.setDocumentId(documentId);
        document.setPositionId(positionId);
        document.setFileName(fileName);
        document.setFileType(contentType);
        StorageObjectPointer pointer;
        try {
            pointer = storageService.upload(StorageCategory.JOB_DOCUMENT, fileName, content, contentType);
        } catch (StorageException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "岗位文档存储失败", e);
        }
        document.setStorageBucket(pointer.bucket());
        document.setStoragePath(pointer.path());
        document.setFileSizeBytes(pointer.sizeBytes());
        document.setUploadUserId(uploaderUserId);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        if (parsingResult != null) {
            document.setParsedTitle(parsingResult.getTitle());
            document.setParsedLocation(parsingResult.getLocation());
            document.setParsedPublisher(parsingResult.getPublisherNickname());
            document.setConfidence(parsingResult.getConfidence());
            document.setAiRawResult(parsingResult.getRawJson());
        } else {
            document.setAiRawResult(errorMessage);
            document.setConfidence(null);
        }
        jobDocumentRepository.save(document);
        entity.setDocumentId(documentId);
        recruitingPositionRepository.save(entity);

        return toDetail(entity, document);
    }

    @Transactional
    public JobDetailResponse updatePosition(String positionId, JobUpdateRequest request) {
        CompanyRecruitingPositionEntity entity = recruitingPositionRepository.findById(positionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到岗位"));
        boolean changed = false;
        if (request.getPositionName() != null) {
            if (!StringUtils.hasText(request.getPositionName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "岗位名称不能为空");
            }
            entity.setPositionName(request.getPositionName().trim());
            changed = true;
        }
        if (request.getPositionLocation() != null) {
            entity.setPositionLocation(StringUtils.hasText(request.getPositionLocation())
                    ? request.getPositionLocation().trim() : null);
            changed = true;
        }
        if (request.getPublisherNickname() != null) {
            entity.setPublisherNickname(StringUtils.hasText(request.getPublisherNickname())
                    ? request.getPublisherNickname().trim() : null);
            changed = true;
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
            changed = true;
        } else if (changed) {
            entity.setStatus(RecruitingPositionStatus.READY);
        }
        if (!changed) {
            return getDetail(positionId);
        }
        entity.setUpdatedAt(Instant.now());
        recruitingPositionRepository.save(entity);

        jobDocumentRepository.findByPositionId(positionId).ifPresent(document -> {
            if (request.getPositionName() != null) {
                document.setParsedTitle(entity.getPositionName());
            }
            if (request.getPositionLocation() != null) {
                document.setParsedLocation(entity.getPositionLocation());
            }
            if (request.getPublisherNickname() != null) {
                document.setParsedPublisher(entity.getPublisherNickname());
            }
            document.setUpdatedAt(entity.getUpdatedAt());
            jobDocumentRepository.save(document);
        });

        return getDetail(positionId);
    }

    private JobCardResponse toCard(CompanyRecruitingPositionEntity entity) {
        return new JobCardResponse(entity.getPositionId(),
                entity.getCompanyId(),
                entity.getPositionName(),
                entity.getPositionLocation(),
                entity.getPublisherNickname(),
                entity.getStatus(),
                entity.getUpdatedAt());
    }

    private JobDetailResponse toDetail(CompanyRecruitingPositionEntity entity, CompanyJobDocumentEntity document) {
        JobDocumentResponse documentResponse = null;
        if (document != null) {
            documentResponse = new JobDocumentResponse(document.getDocumentId(),
                    document.getFileName(),
                    document.getFileType(),
                    document.getParsedTitle(),
                    document.getParsedLocation(),
                    document.getParsedPublisher(),
                    document.getConfidence(),
                    document.getAiRawResult(),
                    buildDocumentHtml(document),
                    document.getCreatedAt(),
                    document.getUpdatedAt());
        }
        JobCardResponse card = toCard(entity);
        return new JobDetailResponse(card, entity.getSource(), documentResponse);
    }

    private String buildDocumentHtml(CompanyJobDocumentEntity document) {
        StorageObjectPointer pointer = toPointer(document);
        if (pointer == null) {
            return null;
        }
        byte[] content;
        try {
            content = storageService.download(pointer);
        } catch (StorageException e) {
            log.error("加载岗位文档 {} 失败", document.getDocumentId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "获取岗位文档失败", e);
        }
        if (content == null || content.length == 0) {
            return null;
        }
        String mimeType = StringUtils.hasText(document.getFileType())
                ? document.getFileType()
                : "application/pdf";
        String base64 = Base64.getEncoder().encodeToString(content);
        return "<iframe src=\"data:" + mimeType + ";base64," + base64
                + "\" style=\"width:100%;height:100%;border:none;\"></iframe>";
    }

    private StorageObjectPointer toPointer(CompanyJobDocumentEntity document) {
        if (document == null
                || !StringUtils.hasText(document.getStorageBucket())
                || !StringUtils.hasText(document.getStoragePath())) {
            return null;
        }
        return new StorageObjectPointer(document.getStorageBucket(),
                document.getStoragePath(),
                document.getFileSizeBytes(),
                document.getFileType());
    }

    private String resolveCompanyId(String companyId, String userId) {
        if (StringUtils.hasText(companyId)) {
            boolean exists = companyProfileRepository.existsById(companyId);
            if (!exists) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "企业不存在");
            }
            return companyId;
        }
        if (!StringUtils.hasText(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId 或 userId 必须提供");
        }
        return companyProfileRepository.findByOwnerUserId(userId)
                .map(profile -> profile.getCompanyId())
                .or(() -> companyContactRepository.findByUserAccountId(userId)
                        .map(contact -> contact.getCompanyId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到用户对应的企业"));
    }

    private String deriveTitleFromFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "未命名岗位";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot);
        }
        return fileName;
    }
}
