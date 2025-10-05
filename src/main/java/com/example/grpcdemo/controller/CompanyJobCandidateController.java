package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.JobCandidateImportResponse;
import com.example.grpcdemo.controller.dto.JobCandidateListResponse;
import com.example.grpcdemo.controller.dto.JobCandidateListStatus;
import com.example.grpcdemo.service.CompanyJobCandidateService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 岗位候选人导入与查询接口。
 */
@RestController
@RequestMapping("/api/enterprise/jobs")
@Validated
public class CompanyJobCandidateController {

    private final CompanyJobCandidateService candidateService;

    public CompanyJobCandidateController(CompanyJobCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @PostMapping(value = "/{positionId}/candidates/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JobCandidateImportResponse importCandidates(@PathVariable("positionId") String positionId,
                                                       @RequestParam("uploaderUserId") @NotBlank String uploaderUserId,
                                                       @RequestPart("files") MultipartFile[] files) {
        List<MultipartFile> fileList = files != null ? Arrays.asList(files) : List.of();
        return candidateService.importCandidates(positionId, uploaderUserId, fileList);
    }

    @GetMapping("/{positionId}/candidates")
    public JobCandidateListResponse list(@PathVariable("positionId") String positionId,
                                         @RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "status", required = false)
                                         JobCandidateListStatus status,
                                         @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
                                         @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(200) int pageSize) {
        return candidateService.listCandidates(positionId,
                keyword,
                status,
                page,
                pageSize);
    }
}
