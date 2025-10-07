package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.CandidateInterviewQuestionDto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试环境下的占位实现，生成固定问题集合。
 */
@Service
@Profile("test")
public class StubInterviewQuestionClient implements InterviewQuestionClient {

    private final AtomicInteger sequence = new AtomicInteger(1000);

    @Override
    public InterviewQuestionSet fetchQuestions(InterviewQuestionCommand command) {
        CandidateInterviewQuestionDto q1 = new CandidateInterviewQuestionDto();
        q1.setSequence(1);
        q1.setQuestionTitle("请做自我介绍");
        q1.setQuestionDescription("重点说明你的核心技能、项目经验以及职业规划。");

        CandidateInterviewQuestionDto q2 = new CandidateInterviewQuestionDto();
        q2.setSequence(2);
        q2.setQuestionTitle("分享一次你解决技术难题的经历");
        q2.setQuestionDescription("描述背景、你的思路、采取的措施以及最终结果。");

        CandidateInterviewQuestionDto q3 = new CandidateInterviewQuestionDto();
        q3.setSequence(3);
        q3.setQuestionTitle("你对加入本公司的期待是什么？");
        q3.setQuestionDescription("可以从团队氛围、成长机会、技术栈等角度回答。");

        return new InterviewQuestionSet("stub-" + sequence.incrementAndGet(), List.of(q1, q2, q3), Map.of());
    }
}

