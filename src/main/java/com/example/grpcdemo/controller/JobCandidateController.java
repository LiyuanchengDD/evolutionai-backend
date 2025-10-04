package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.JobCandidateInviteRequest;
import com.example.grpcdemo.controller.dto.JobCandidateItemResponse;
import com.example.grpcdemo.controller.dto.JobCandidateResumeResponse;
import com.example.grpcdemo.controller.dto.JobCandidateUpdateRequest;
import com.example.grpcdemo.service.CompanyJobCandidateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
