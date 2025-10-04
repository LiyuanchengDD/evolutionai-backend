package com.example.grpcdemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Base64;

/**
 * 默认实现，调用外部 AI 简历解析服务。
 */
@Service
@Profile("!test")
public class RestResumeParser implements ResumeParser {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RestResumeParser(WebClient.Builder builder,
                            @Value("${ai.resume-parser.base-url}") String baseUrl,
                            ObjectMapper objectMapper) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ResumeParsingResult parse(ResumeParsingCommand command) {
        try {
            String content = Base64.getEncoder().encodeToString(command.getFileContent());
            ResumeParserResponse body = webClient.post()
                    .uri("/resumes:parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ResumeParserRequest(command.getFileName(),
                            command.getContentType(),
                            command.getCompanyId(),
                            command.getPositionId(),
                            command.getUploaderUserId(),
                            content))
                    .retrieve()
                    .bodyToMono(ResumeParserResponse.class)
                    .block(Duration.ofSeconds(20));

            if (body == null) {
                throw new ResumeParsingException("AI 简历解析服务返回空响应");
            }
            String rawJson = null;
            if (body.raw() != null) {
                rawJson = objectMapper.writeValueAsString(body.raw());
            }
            return new ResumeParsingResult(body.name(), body.email(), body.phone(), body.htmlContent(), body.confidence(), rawJson);
        } catch (WebClientResponseException e) {
            throw new ResumeParsingException("AI 简历解析服务返回错误: " + e.getStatusCode(), e);
        } catch (ResumeParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new ResumeParsingException("调用 AI 简历解析服务失败", e);
        }
    }

    private record ResumeParserRequest(String fileName,
                                       String contentType,
                                       String companyId,
                                       String positionId,
                                       String uploaderUserId,
                                       String contentBase64) {
    }

    private record ResumeParserResponse(String name,
                                        String email,
                                        String phone,
                                        String htmlContent,
                                        Float confidence,
                                        Object raw) {
    }
}
