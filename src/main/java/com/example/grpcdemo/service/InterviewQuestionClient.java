package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.CandidateInterviewQuestionDto;

import java.util.List;
import java.util.Map;

/**
 * 封装面试题目的生成能力。
 */
public interface InterviewQuestionClient {

    InterviewQuestionSet fetchQuestions(InterviewQuestionCommand command);

    record InterviewQuestionCommand(String jobCandidateId,
                                    String positionId,
                                    String candidateName,
                                    String positionName,
                                    String companyName,
                                    String locale,
                                    Map<String, Object> context) {
    }

    record InterviewQuestionSet(String sessionId,
                                List<CandidateInterviewQuestionDto> questions,
                                Map<String, Object> metadata) {
    }
}

