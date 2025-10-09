package com.example.grpcdemo.controller;

import com.example.grpcdemo.controller.dto.AiExtractionRequest;
import com.example.grpcdemo.controller.dto.AiQuestionGenerationRequest;
import com.example.grpcdemo.controller.dto.AiQuestionGenerationResponse;
import com.example.grpcdemo.controller.dto.ApiResponse;
import com.example.grpcdemo.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能体开放接口。
 */
@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/questions_generator")
    public ResponseEntity<ApiResponse<AiQuestionGenerationResponse>> generateQuestions(@Valid @RequestBody AiQuestionGenerationRequest request) {
        AiQuestionGenerationResponse response = aiAssistantService.generateQuestions(request);
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<?>> extract(@Valid @RequestBody AiExtractionRequest request) {
        return switch (request.getExtractType()) {
            case "1" -> ResponseEntity.ok(new ApiResponse<>(aiAssistantService.extractResume(request.getFileUrl())));
            case "2" -> ResponseEntity.ok(new ApiResponse<>(aiAssistantService.extractJob(request.getFileUrl())));
            default -> throw aiAssistantService.unsupportedExtractType(request.getExtractType());
        };
    }
}
