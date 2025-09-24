package com.example.grpcdemo.service;

/**
 * Simple DTO holding AI evaluation results.
 *
 * @param content textual report content
 * @param score numerical score from 0 to 1
 * @param comment evaluator comments
 */
public record EvaluationResult(String content, float score, String comment) {
}

