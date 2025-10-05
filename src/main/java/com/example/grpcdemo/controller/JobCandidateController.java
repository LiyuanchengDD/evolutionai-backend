package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.CandidateAiEvaluationRequest;
import com.example.grpcdemo.controller.dto.CandidateAiEvaluationResponse;
import com.example.grpcdemo.controller.dto.CandidateInterviewRecordRequest;
import com.example.grpcdemo.controller.dto.CandidateInterviewRecordResponse;
import com.example.grpcdemo.controller.dto.JobCandidateInviteRequest;
import com.example.grpcdemo.controller.dto.JobCandidateItemResponse;
import com.example.grpcdemo.controller.dto.JobCandidateResumeResponse;
import com.example.grpcdemo.controller.dto.JobCandidateUpdateRequest;
import com.example.grpcdemo.service.CompanyJobCandidateService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 候选人详情与操作接口。
 */
@RestController
@RequestMapping("/api/enterprise/job-candidates")
public class JobCandidateController {

    private final CompanyJobCandidateService candidateService;

    public JobCandidateController(CompanyJobCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping("/{jobCandidateId}/resume")
    public JobCandidateResumeResponse resume(@PathVariable("jobCandidateId") String jobCandidateId) {
        return candidateService.getResume(jobCandidateId);
    }

    @GetMapping("/{jobCandidateId}/resume/file")
    public ResponseEntity<ByteArrayResource> resumeFile(@PathVariable("jobCandidateId") String jobCandidateId) {
        CompanyJobCandidateService.ResumeFilePayload file = candidateService.getResumeFile(jobCandidateId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.fileType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_PDF;
        }
        ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        ByteArrayResource resource = new ByteArrayResource(file.content());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    @PatchMapping("/{jobCandidateId}")
    public JobCandidateItemResponse update(@PathVariable("jobCandidateId") String jobCandidateId,
                                           @Valid @RequestBody JobCandidateUpdateRequest request) {
        return candidateService.updateCandidate(jobCandidateId, request);
    }

    @PostMapping("/{jobCandidateId}/invite")
    public JobCandidateItemResponse sendInvite(@PathVariable("jobCandidateId") String jobCandidateId,
                                               @Valid @RequestBody(required = false) JobCandidateInviteRequest request) {
        return candidateService.sendInvite(jobCandidateId, request);
    }

    @GetMapping("/{jobCandidateId}/interview-record")
    public CandidateInterviewRecordResponse interviewRecord(@PathVariable("jobCandidateId") String jobCandidateId) {
        return candidateService.getInterviewRecord(jobCandidateId);
    }

    @PutMapping("/{jobCandidateId}/interview-record")
    public CandidateInterviewRecordResponse upsertInterviewRecord(@PathVariable("jobCandidateId") String jobCandidateId,
                                                                  @Valid @RequestBody(required = false) CandidateInterviewRecordRequest request) {
        return candidateService.upsertInterviewRecord(jobCandidateId, request);
    }

    @GetMapping("/{jobCandidateId}/ai-evaluation")
    public CandidateAiEvaluationResponse aiEvaluation(@PathVariable("jobCandidateId") String jobCandidateId) {
        return candidateService.getAiEvaluation(jobCandidateId);
    }

    @PutMapping("/{jobCandidateId}/ai-evaluation")
    public CandidateAiEvaluationResponse upsertAiEvaluation(@PathVariable("jobCandidateId") String jobCandidateId,
                                                            @Valid @RequestBody(required = false) CandidateAiEvaluationRequest request) {
        return candidateService.upsertAiEvaluation(jobCandidateId, request);
    }
}
