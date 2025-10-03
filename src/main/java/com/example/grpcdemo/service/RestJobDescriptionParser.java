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
 * 默认实现，调用外部 AI 服务解析岗位 JD。
 */
@Service
@Profile("!test")
public class RestJobDescriptionParser implements JobDescriptionParser {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RestJobDescriptionParser(WebClient.Builder builder,
                                    @Value("${ai.job-parser.base-url}") String baseUrl,
                                    ObjectMapper objectMapper) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public JobParsingResult parse(JobParsingCommand command) {
        try {
            String content = Base64.getEncoder().encodeToString(command.getFileContent());
            JobParserResponse body = webClient.post()
                    .uri("/jobs:parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new JobParserRequest(command.getFileName(),
                            command.getContentType(),
                            command.getCompanyId(),
                            command.getUploaderUserId(),
                            content))
                    .retrieve()
                    .bodyToMono(JobParserResponse.class)
                    .block(Duration.ofSeconds(15));

            if (body == null) {
                throw new JobParsingException("AI 解析服务返回空响应");
            }
            String rawJson = null;
            if (body.raw() != null) {
                rawJson = objectMapper.writeValueAsString(body.raw());
            }
            return new JobParsingResult(body.title(), body.location(), body.publisherNickname(), body.confidence(), rawJson);
        } catch (WebClientResponseException e) {
            throw new JobParsingException("AI 解析服务返回错误: " + e.getStatusCode(), e);
        } catch (JobParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new JobParsingException("调用 AI 解析服务失败", e);
        }
    }

    private record JobParserRequest(String fileName,
                                    String contentType,
                                    String companyId,
                                    String uploaderUserId,
                                    String contentBase64) {
    }

    private record JobParserResponse(String title,
                                     String location,
                                     String publisherNickname,
                                     Float confidence,
                                     Object raw) {
    }
}
