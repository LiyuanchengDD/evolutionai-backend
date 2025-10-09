package com.example.grpcdemo.controller.dto;

import java.util.List;

/**
 * 生成面试问题响应体。
 */
public class AiQuestionGenerationResponse {

    private List<String> questions;

    public AiQuestionGenerationResponse() {
    }

    public AiQuestionGenerationResponse(List<String> questions) {
        this.questions = questions;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }
}
