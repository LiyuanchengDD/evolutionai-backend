package com.example.grpcdemo.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 测试环境使用的简易解析器，避免依赖真实 AI 服务。
 */
@Service
@Profile("test")
public class StubJobDescriptionParser implements JobDescriptionParser {

    @Override
    public JobParsingResult parse(JobParsingCommand command) {
        String fallbackTitle = "岗位" + UUID.randomUUID().toString().substring(0, 8);
        String fileName = command.getFileName();
        if (fileName != null && !fileName.isBlank()) {
            fallbackTitle = fileName.replaceAll("\\.pdf$", "");
        }
        String raw = new String(command.getFileContent(), StandardCharsets.UTF_8);
        return new JobParsingResult(fallbackTitle, "未设置地点", "系统", 0.0f, raw);
    }
}
