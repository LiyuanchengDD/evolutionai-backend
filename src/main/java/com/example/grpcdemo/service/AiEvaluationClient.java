package com.example.grpcdemo.service;

/**
 * Client used to obtain AI evaluation results for interviews.
 */
public interface AiEvaluationClient {

    /**
     * Evaluate an interview and return analysis results.
     *
     * @param interviewId the interview identifier
     * @return the evaluation result
     */
    EvaluationResult evaluate(String interviewId);
}

