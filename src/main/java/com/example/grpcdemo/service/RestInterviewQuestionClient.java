package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.CandidateInterviewQuestionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * 默认实现：调用外部 AI 智能体生成题目。
 */
@Service
@Profile("!test")
public class RestInterviewQuestionClient implements InterviewQuestionClient {

    private final WebClient webClient;

    public RestInterviewQuestionClient(WebClient.Builder builder,
                                       @Value("${ai.interview-service.base-url}") String baseUrl) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public InterviewQuestionSet fetchQuestions(InterviewQuestionCommand command) {
        try {
            RestResponse body = webClient.post()
                    .uri("/interviews/questions:generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new RestRequest(command.jobCandidateId(),
                            command.positionId(),
                            command.candidateName(),
                            command.positionName(),
                            command.companyName(),
                            command.locale(),
                            command.context()))
                    .retrieve()
                    .bodyToMono(RestResponse.class)
                    .block(Duration.ofSeconds(15));

            if (body == null || body.questions() == null || body.questions().isEmpty()) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 题目服务返回空结果");
            }
            List<CandidateInterviewQuestionDto> questions = body.questions().stream()
                    .map(RestQuestion::toDto)
                    .collect(Collectors.toList());
            return new InterviewQuestionSet(body.sessionId(), questions, body.metadata());
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI 题目服务返回错误: " + e.getStatusCode(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "调用 AI 题目服务失败", e);
        }
    }

    private record RestRequest(String jobCandidateId,
                               String positionId,
                               String candidateName,
                               String positionName,
                               String companyName,
                               String locale,
                               Map<String, Object> context) {
    }

    private record RestQuestion(Integer sequence,
                                String title,
                                String description,
                                Map<String, Object> metadata) {

        private CandidateInterviewQuestionDto toDto() {
            CandidateInterviewQuestionDto dto = new CandidateInterviewQuestionDto();
            dto.setSequence(sequence);
            dto.setQuestionTitle(title);
            dto.setQuestionDescription(description);
            if (metadata != null && metadata.containsKey("score")) {
                Object score = metadata.get("score");
                if (score instanceof Number number) {
                    dto.setQuestionScore(number.intValue());
                }
            }
            return dto;
        }
    }

    private record RestResponse(String sessionId,
                                List<RestQuestion> questions,
                                Map<String, Object> metadata) {
    }
}

