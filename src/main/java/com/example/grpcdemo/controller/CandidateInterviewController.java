package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.CandidateInterviewAbandonRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewAnswerRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewAnswerResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewBeginResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewDetailResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewCompleteRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewInvitationItem;
import com.example.grpcdemo.controller.dto.CandidateInterviewInvitationListResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewPrecheckRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewProfilePhotoDto;
import com.example.grpcdemo.controller.dto.CandidateInterviewProfilePhotoRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewRecordResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewStartRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewStartResponse;
import com.example.grpcdemo.service.CandidateInterviewPortalService;
import com.example.grpcdemo.service.CompanyJobCandidateService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 候选人面试门户接口。
 */
@RestController
@RequestMapping("/api/candidate/interview-sessions")
public class CandidateInterviewController {

    private final CandidateInterviewPortalService portalService;

    public CandidateInterviewController(CandidateInterviewPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/{jobCandidateId}/invitations")
    public CandidateInterviewInvitationListResponse invitations(@PathVariable("jobCandidateId") String jobCandidateId,
                                                                 @RequestParam(value = "status", required = false) String status,
                                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                                 Locale locale) {
        return portalService.listInvitations(jobCandidateId, status, keyword, locale);
    }

    @GetMapping("/{jobCandidateId}")
    public CandidateInterviewDetailResponse detail(@PathVariable("jobCandidateId") String jobCandidateId,
                                                   Locale locale) {
        return portalService.getInvitationDetail(jobCandidateId, locale);
    }

    @PostMapping("/{jobCandidateId}/precheck")
    public CandidateInterviewDetailResponse submitPrecheck(@PathVariable("jobCandidateId") String jobCandidateId,
                                                           @Valid @RequestBody CandidateInterviewPrecheckRequest request,
                                                           Locale locale) {
        return portalService.submitPrecheck(jobCandidateId, request, locale);
    }

    @PostMapping("/{jobCandidateId}/start")
    public CandidateInterviewStartResponse start(@PathVariable("jobCandidateId") String jobCandidateId,
                                                 @RequestBody(required = false) CandidateInterviewStartRequest request) {
        return portalService.startInterview(jobCandidateId, request);
    }

    @PostMapping("/{jobCandidateId}/begin-answering")
    public CandidateInterviewBeginResponse beginAnswering(@PathVariable("jobCandidateId") String jobCandidateId) {
        return portalService.beginAnswering(jobCandidateId);
    }

    @PostMapping("/{jobCandidateId}/pause")
    public CandidateInterviewBeginResponse pauseAnswering(@PathVariable("jobCandidateId") String jobCandidateId) {
        return portalService.pauseAnswering(jobCandidateId);
    }

    @PostMapping("/{jobCandidateId}/answers")
    public CandidateInterviewAnswerResponse submitAnswer(@PathVariable("jobCandidateId") String jobCandidateId,
                                                         @Valid @RequestBody CandidateInterviewAnswerRequest request) {
        return portalService.submitAnswer(jobCandidateId, request);
    }

    @PostMapping("/{jobCandidateId}/complete")
    public CandidateInterviewRecordResponse complete(@PathVariable("jobCandidateId") String jobCandidateId,
                                                     @RequestBody(required = false) CandidateInterviewCompleteRequest request) {
        return portalService.completeInterview(jobCandidateId, request);
    }

    @PostMapping("/{jobCandidateId}/abandon")
    public CandidateInterviewInvitationItem abandon(@PathVariable("jobCandidateId") String jobCandidateId,
                                                    @RequestBody(required = false) CandidateInterviewAbandonRequest request,
                                                    Locale locale) {
        return portalService.abandonInterview(jobCandidateId, request, locale);
    }

    @PostMapping("/{jobCandidateId}/profile-photo")
    public CandidateInterviewProfilePhotoDto uploadProfilePhoto(@PathVariable("jobCandidateId") String jobCandidateId,
                                                                @Valid @RequestBody CandidateInterviewProfilePhotoRequest request) {
        return portalService.uploadProfilePhoto(jobCandidateId, request);
    }

    @GetMapping("/{jobCandidateId}/profile-photo")
    public ResponseEntity<ByteArrayResource> profilePhoto(@PathVariable("jobCandidateId") String jobCandidateId) {
        CompanyJobCandidateService.InterviewProfilePhotoPayload photo = portalService.downloadProfilePhoto(jobCandidateId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(photo.fileType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.IMAGE_JPEG;
        }
        ContentDisposition disposition = ContentDisposition.builder("inline")
                .filename(photo.fileName(), StandardCharsets.UTF_8)
                .build();
        ByteArrayResource resource = new ByteArrayResource(photo.content());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(photo.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @GetMapping("/{jobCandidateId}/audios/{audioId}")
    public ResponseEntity<ByteArrayResource> answerAudio(@PathVariable("jobCandidateId") String jobCandidateId,
                                                         @PathVariable("audioId") String audioId) {
        CompanyJobCandidateService.InterviewAudioPayload audio = portalService.downloadAudio(jobCandidateId, audioId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(audio.fileType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.builder("inline")
                .filename(audio.fileName(), StandardCharsets.UTF_8)
                .build();
        ByteArrayResource resource = new ByteArrayResource(audio.content());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(audio.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}

