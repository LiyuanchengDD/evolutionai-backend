package com.example.grpcdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * 测试环境使用的占位解析器，直接从文件名生成基础信息。
 */
@Service
@Profile("test")
public class StubResumeParser implements ResumeParser {

    private static final Logger log = LoggerFactory.getLogger(StubResumeParser.class);

    @Override
    public ResumeParsingResult parse(ResumeParsingCommand command) {
        String fileName = command.getFileName() != null ? command.getFileName() : "unknown.pdf";
        String candidateName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        if (candidateName.length() > 50) {
            candidateName = candidateName.substring(0, 50);
        }
        String html = "<p>Stub resume preview for " + candidateName + "</p>";
        String raw = Base64.getEncoder().encodeToString(command.getFileContent());
        log.info("Stub resume parser invoked for file {}", fileName);
        return new ResumeParsingResult(candidateName, candidateName + "@example.com", "", html, 0.0f, raw);
    }
}
