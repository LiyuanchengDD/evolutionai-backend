package com.example.grpcdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Default implementation that delegates to the external AI evaluation service
 * over HTTP.
 */
@Service
@Profile("!test")
public class RestAiEvaluationClient implements AiEvaluationClient {

    private final WebClient webClient;

    public RestAiEvaluationClient(WebClient.Builder builder,
                                  @Value("${ai.evaluation-service.base-url}") String baseUrl) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public EvaluationResult evaluate(String interviewId) {
        try {
            AiEvaluationResponse body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/interviews/{interviewId}/analysis")
                            .build(interviewId))
                    .retrieve()
                    .bodyToMono(AiEvaluationResponse.class)
                    .block(Duration.ofSeconds(10));

            if (body == null) {
                throw new IllegalStateException("AI evaluation response body was empty");
            }
            if (body.content() == null || body.content().isBlank()) {
                throw new IllegalStateException("AI evaluation response did not contain report content");
            }

            float score = body.score() != null ? body.score() : 0.0f;
            String comment = body.comment() != null ? body.comment() : "";
            return new EvaluationResult(body.content(), score, comment);
        } catch (WebClientResponseException e) {
            throw new RuntimeException("AI evaluation service returned %s".formatted(e.getStatusCode()), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call AI evaluation service", e);
        }
    }

    private record AiEvaluationResponse(String content, Float score, String comment) {
    }
}
