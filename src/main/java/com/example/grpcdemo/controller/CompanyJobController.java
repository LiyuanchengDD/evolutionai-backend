package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.JobCardResponse;
import com.example.grpcdemo.controller.dto.JobDetailResponse;
import com.example.grpcdemo.controller.dto.JobSummaryResponse;
import com.example.grpcdemo.controller.dto.JobUpdateRequest;
import com.example.grpcdemo.service.CompanyJobService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

/**
 * 企业岗位管理相关的 REST API。
 */
@RestController
@RequestMapping("/api/enterprise/jobs")
public class CompanyJobController {

    private final CompanyJobService companyJobService;

    public CompanyJobController(CompanyJobService companyJobService) {
        this.companyJobService = companyJobService;
    }

    @GetMapping("/summary")
    public JobSummaryResponse summary(@RequestParam(value = "companyId", required = false) String companyId,
                                      @RequestParam(value = "userId", required = false) String userId) {
        return companyJobService.loadSummary(companyId, userId);
    }

    @GetMapping
    public List<JobCardResponse> list(@RequestParam(value = "companyId", required = false) String companyId,
                                      @RequestParam(value = "userId", required = false) String userId) {
        return companyJobService.listCards(companyId, userId);
    }

    @GetMapping("/{positionId}")
    public JobDetailResponse detail(@PathVariable("positionId") String positionId) {
        return companyJobService.getDetail(positionId);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JobDetailResponse importJob(@RequestParam(value = "companyId", required = false) String companyId,
                                       @RequestParam(value = "userId", required = false) String userId,
                                       @RequestParam("uploaderUserId") String uploaderUserId,
                                       @RequestPart("file") MultipartFile file) {
        try {
            return companyJobService.importPosition(companyId,
                    userId,
                    uploaderUserId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取上传文件失败", e);
        }
    }

    @PatchMapping("/{positionId}")
    public JobDetailResponse update(@PathVariable("positionId") String positionId,
                                    @Valid @RequestBody JobUpdateRequest request) {
        return companyJobService.updatePosition(positionId, request);
    }
}
