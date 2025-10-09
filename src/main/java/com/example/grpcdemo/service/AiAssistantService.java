package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.AiQuestionGenerationRequest;
import com.example.grpcdemo.controller.dto.AiQuestionGenerationResponse;
import com.example.grpcdemo.controller.dto.JobExtractionResponse;
import com.example.grpcdemo.controller.dto.ResumeExtractionResponse;
import com.example.grpcdemo.entity.AiInterviewQuestionSetEntity;
import com.example.grpcdemo.entity.AiJobExtractionEntity;
import com.example.grpcdemo.entity.AiResumeExtractionEntity;
import com.example.grpcdemo.repository.AiInterviewQuestionSetRepository;
import com.example.grpcdemo.repository.AiJobExtractionRepository;
import com.example.grpcdemo.repository.AiResumeExtractionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.ContentHandler;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * AI 智能体业务逻辑，负责文件下载、解析与题目生成。
 */
@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:(?:\\+?86)?[- ]?)?1[3-9]\\d{9}");

    private final WebClient webClient;
    private final Parser parser;
    private final Tika tika;
    private final AiResumeExtractionRepository resumeExtractionRepository;
    private final AiJobExtractionRepository jobExtractionRepository;
    private final AiInterviewQuestionSetRepository questionSetRepository;
    private final ObjectMapper objectMapper;

    public AiAssistantService(WebClient.Builder builder,
                              AiResumeExtractionRepository resumeExtractionRepository,
                              AiJobExtractionRepository jobExtractionRepository,
                              AiInterviewQuestionSetRepository questionSetRepository,
                              ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.parser = new AutoDetectParser();
        this.tika = new Tika();
        this.resumeExtractionRepository = resumeExtractionRepository;
        this.jobExtractionRepository = jobExtractionRepository;
        this.questionSetRepository = questionSetRepository;
        this.objectMapper = objectMapper;
    }

    public AiQuestionGenerationResponse generateQuestions(AiQuestionGenerationRequest request) {
        int questionNum = request.getQuestionNum() != null ? request.getQuestionNum() : 10;
        DownloadedFile resumeFile = fetchFile(request.getResumeUrl());
        DownloadedFile jdFile = fetchFile(request.getJdUrl());
        String resumeText = safeExtractText(resumeFile);
        String jdText = safeExtractText(jdFile);

        String candidateName = extractName(resumeText);
        String contactEmail = extractEmail(resumeText);
        String jobTitle = extractJobTitle(jdText);
        String jobLocation = extractJobLocation(jdText);

        List<String> questions = buildQuestionList(questionNum, candidateName, jobTitle, jobLocation);
        persistQuestionSet(request, questions, resumeText, jdText, candidateName, contactEmail, jobTitle, jobLocation);
        return new AiQuestionGenerationResponse(questions);
    }

    public ResumeExtractionResponse extractResume(String fileUrl) {
        DownloadedFile file = fetchFile(fileUrl);
        String text = safeExtractText(file);
        String email = extractEmail(text);
        String phone = extractPhone(text);
        String name = extractName(text);

        AiResumeExtractionEntity entity = new AiResumeExtractionEntity();
        entity.setExtractionId(UUID.randomUUID().toString());
        entity.setFileUrl(fileUrl);
        entity.setFileType(file.contentType());
        entity.setExtractedEmail(email);
        entity.setExtractedPhone(phone);
        entity.setExtractedName(name);
        entity.setRawText(text);
        entity.setCreatedAt(Instant.now());
        resumeExtractionRepository.save(entity);
        return new ResumeExtractionResponse(email, name, phone);
    }

    public JobExtractionResponse extractJob(String fileUrl) {
        DownloadedFile file = fetchFile(fileUrl);
        String text = safeExtractText(file);
        String title = extractJobTitle(text);
        String location = extractJobLocation(text);

        AiJobExtractionEntity entity = new AiJobExtractionEntity();
        entity.setExtractionId(UUID.randomUUID().toString());
        entity.setFileUrl(fileUrl);
        entity.setFileType(file.contentType());
        entity.setExtractedTitle(title);
        entity.setExtractedLocation(location);
        entity.setRawText(text);
        entity.setCreatedAt(Instant.now());
        jobExtractionRepository.save(entity);
        return new JobExtractionResponse(location, title);
    }

    public ResponseStatusException unsupportedExtractType(String extractType) {
        return new ResponseStatusException(BAD_REQUEST, "不支持的提取类型: " + extractType);
    }

    private void persistQuestionSet(AiQuestionGenerationRequest request,
                                    List<String> questions,
                                    String resumeText,
                                    String jdText,
                                    String candidateName,
                                    String candidateEmail,
                                    String jobTitle,
                                    String jobLocation) {
        AiInterviewQuestionSetEntity entity = new AiInterviewQuestionSetEntity();
        entity.setRecordId(UUID.randomUUID().toString());
        entity.setResumeUrl(request.getResumeUrl());
        entity.setJdUrl(request.getJdUrl());
        entity.setQuestionNum(request.getQuestionNum() != null ? request.getQuestionNum() : questions.size());
        entity.setCandidateName(candidateName);
        entity.setCandidateEmail(candidateEmail);
        entity.setJobTitle(jobTitle);
        entity.setJobLocation(jobLocation);
        entity.setResumeSnapshot(resumeText != null && resumeText.length() > 2000 ? resumeText.substring(0, 2000) : resumeText);
        entity.setJdSnapshot(jdText != null && jdText.length() > 2000 ? jdText.substring(0, 2000) : jdText);
        entity.setCreatedAt(Instant.now());
        try {
            entity.setQuestionsJson(objectMapper.writeValueAsString(questions));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "题目序列化失败", e);
        }
        questionSetRepository.save(entity);
    }

    private DownloadedFile fetchFile(String url) {
        try {
            ResponseEntity<byte[]> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(byte[].class)
                    .block(Duration.ofSeconds(30));
            if (response == null || response.getBody() == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "文件下载失败");
            }
            MediaType mediaType = response.getHeaders().getContentType();
            String contentType = mediaType != null ? mediaType.toString() : detectContentType(url, response.getBody());
            return new DownloadedFile(response.getBody(), contentType);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "获取文件内容失败", e);
        }
    }

    private String detectContentType(String url, byte[] data) {
        try {
            String detected = tika.detect(data, url);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (Exception e) {
            log.debug("无法通过 Tika 检测内容类型: {}", e.getMessage());
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String safeExtractText(DownloadedFile file) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(file.content())) {
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            if (StringUtils.hasText(file.contentType())) {
                metadata.set(Metadata.CONTENT_TYPE, file.contentType());
            }
            parser.parse(inputStream, handler, metadata, new ParseContext());
            String text = handler.toString();
            if (!StringUtils.hasText(text) && isPlainText(file.contentType())) {
                return new String(file.content());
            }
            return text;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            if (isPlainText(file.contentType())) {
                return new String(file.content());
            }
            throw new ResponseStatusException(BAD_GATEWAY, "解析文件内容失败", e);
        }
    }

    private boolean isPlainText(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.toLowerCase(Locale.ROOT).contains("text");
    }

    private String extractEmail(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractPhone(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = PHONE_PATTERN.matcher(text.replaceAll("\\s", ""));
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractName(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] lines = text.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (EMAIL_PATTERN.matcher(line).find()) {
                continue;
            }
            if (line.contains(":") || line.contains("：") || line.matches(".*@.*")) {
                continue;
            }
            if (line.length() <= 32) {
                return line;
            }
        }
        return null;
    }

    private String extractJobTitle(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] lines = text.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("职位") || line.startsWith("岗位") || line.toLowerCase(Locale.ROOT).startsWith("title")) {
                return normalizeValueAfterDelimiter(line);
            }
        }
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return null;
    }

    private String extractJobLocation(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] lines = text.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (line.startsWith("地点") || line.startsWith("工作地") || line.startsWith("地址") || lower.startsWith("location")) {
                return normalizeValueAfterDelimiter(line);
            }
        }
        return null;
    }

    private String normalizeValueAfterDelimiter(String line) {
        int index = Math.max(Math.max(line.indexOf(':'), line.indexOf('：')), -1);
        if (index >= 0 && index < line.length() - 1) {
            return line.substring(index + 1).trim();
        }
        return line.trim();
    }

    private List<String> buildQuestionList(int questionNum, String candidateName, String jobTitle, String jobLocation) {
        String name = StringUtils.hasText(candidateName) ? candidateName : "您";
        String title = StringUtils.hasText(jobTitle) ? jobTitle : "该岗位";
        String location = StringUtils.hasText(jobLocation) ? jobLocation : "工作地";

        List<String> questions = new ArrayList<>();
        questions.add(String.format("请先做一下自我介绍，重点分享您在%s相关的经历。", title));
        questions.add(String.format("结合您在%s的经验，谈谈最具代表性的一个项目以及取得的成果。", title));
        questions.add(String.format("在从事%s工作时，您曾遇到过哪些挑战？您是如何解决的？", title));
        questions.add(String.format("针对我们位于%s的团队，您认为自己适应当地业务或文化的优势是什么？", location));
        questions.add("请分享一次与跨团队协作的经历，重点说明沟通方式和最终成效。");
        questions.add("谈谈您近期关注的行业动态或技术趋势，它们如何影响到您的职业规划？");
        questions.add(String.format("如果加入我们担任%s，您希望前三个月完成哪些目标？", title));
        questions.add(String.format("请回顾简历中最能体现您领导力的一次经历，%s在其中起到了什么作用？", name));
        questions.add("请举例说明您如何通过学习或培训快速提升某项核心能力。");
        questions.add(String.format("针对岗位要求的关键技能，您认为自己还需要加强哪些方面？计划如何提升？"));

        if (questionNum < questions.size()) {
            return new ArrayList<>(questions.subList(0, questionNum));
        }
        int index = 0;
        while (questions.size() < questionNum) {
            index++;
            questions.add(String.format("请分享第 %d 次让您印象深刻的团队合作经历，以及您在其中的贡献。", index));
        }
        return questions;
    }

    private record DownloadedFile(byte[] content, String contentType) {
    }
}
