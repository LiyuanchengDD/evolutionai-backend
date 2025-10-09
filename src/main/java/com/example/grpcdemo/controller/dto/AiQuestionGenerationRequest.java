package com.example.grpcdemo.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 生成面试问题请求体。
 */
public class AiQuestionGenerationRequest {

    @NotBlank(message = "简历地址不能为空")
    private String resumeUrl;

    @NotBlank(message = "职位描述地址不能为空")
    private String jdUrl;

    @Min(value = 1, message = "问题数量至少为 1")
    @Max(value = 50, message = "问题数量最多为 50")
    private Integer questionNum = 10;

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getJdUrl() {
        return jdUrl;
    }

    public void setJdUrl(String jdUrl) {
        this.jdUrl = jdUrl;
    }

    public Integer getQuestionNum() {
        return questionNum;
    }

    public void setQuestionNum(Integer questionNum) {
        this.questionNum = questionNum;
    }
}
