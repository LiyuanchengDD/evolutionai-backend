package com.example.grpcdemo.service;

/**
 * 定义岗位 JD 文本解析行为的接口。
 */
public interface JobDescriptionParser {

    /**
     * 调用外部 AI 服务解析上传的 JD 文档。
     *
     * @param command 解析指令，包含文件内容及元数据
     * @return 解析后的结果快照
     * @throws JobParsingException 当解析失败或服务不可用时抛出
     */
    JobParsingResult parse(JobParsingCommand command);
}
