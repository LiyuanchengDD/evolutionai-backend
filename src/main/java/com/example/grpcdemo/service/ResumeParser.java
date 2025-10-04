package com.example.grpcdemo.service;

/**
 * 定义简历解析行为的接口。
 */
public interface ResumeParser {

    /**
     * 调用外部 AI 服务解析上传的简历。
     *
     * @param command 解析命令
     * @return 解析结果
     */
    ResumeParsingResult parse(ResumeParsingCommand command);
}
