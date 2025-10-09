package com.example.grpcdemo.service;

import com.example.grpcdemo.controller.dto.AiQuestionGenerationRequest;
import com.example.grpcdemo.controller.dto.AiQuestionGenerationResponse;
import com.example.grpcdemo.controller.dto.JobExtractionResponse;
import com.example.grpcdemo.controller.dto.ResumeExtractionResponse;
import com.example.grpcdemo.entity.AiInterviewQuestionSetEntity;
import com.example.grpcdemo.entity.AiJobExtractionEntity;
import com.example.grpcdemo.entity.AiQuestionTemplateEntity;
import com.example.grpcdemo.entity.AiResumeExtractionEntity;
import com.example.grpcdemo.repository.AiInterviewQuestionSetRepository;
import com.example.grpcdemo.repository.AiJobExtractionRepository;
import com.example.grpcdemo.repository.AiQuestionTemplateRepository;
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

    private static final String DEFAULT_LANGUAGE = "zh-CN";

    private final WebClient webClient;
    private final Parser parser;
    private final Tika tika;
    private final AiResumeExtractionRepository resumeExtractionRepository;
    private final AiJobExtractionRepository jobExtractionRepository;
    private final AiInterviewQuestionSetRepository questionSetRepository;
    private final AiQuestionTemplateRepository questionTemplateRepository;
    private final ObjectMapper objectMapper;

    public AiAssistantService(WebClient.Builder builder,
                              AiResumeExtractionRepository resumeExtractionRepository,
                              AiJobExtractionRepository jobExtractionRepository,
                              AiInterviewQuestionSetRepository questionSetRepository,
                              AiQuestionTemplateRepository questionTemplateRepository,
                              ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.parser = new AutoDetectParser();
        this.tika = new Tika();
        this.resumeExtractionRepository = resumeExtractionRepository;
        this.jobExtractionRepository = jobExtractionRepository;
        this.questionSetRepository = questionSetRepository;
        this.questionTemplateRepository = questionTemplateRepository;
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
        List<AiQuestionTemplateEntity> templates = loadTemplates();
        List<String> questions = new ArrayList<>(questionNum);
        TemplateContext context = new TemplateContext(candidateName, jobTitle, jobLocation);

        int iteration = 0;
        int maxAttempts = questionNum * templates.size() * 2;
        while (questions.size() < questionNum && iteration < maxAttempts) {
            AiQuestionTemplateEntity template = templates.get(iteration % templates.size());
            int sequence = questions.size() + 1;
            int repeatIndex = iteration / templates.size();
            String rendered = renderTemplate(template.getContent(), context, sequence, repeatIndex);
            if (StringUtils.hasText(rendered)) {
                questions.add(rendered);
            }
            iteration++;
        }

        if (questions.size() < questionNum) {
            throw new ResponseStatusException(BAD_GATEWAY, "面试问题模版配置不完整，无法生成足够的题目");
        }
        return questions.size() == questionNum ? questions : new ArrayList<>(questions.subList(0, questionNum));
    }

    private List<AiQuestionTemplateEntity> loadTemplates() {
        List<AiQuestionTemplateEntity> templates = questionTemplateRepository
                .findByLanguageAndActiveTrueOrderByDisplayOrderAsc(DEFAULT_LANGUAGE);
        if (templates.isEmpty()) {
            templates = questionTemplateRepository.findByActiveTrueOrderByDisplayOrderAsc();
        }
        if (templates.isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "没有配置可用的面试问题模版");
        }
        return templates;
    }

    private String renderTemplate(String template,
                                  TemplateContext context,
                                  int sequence,
                                  int repeatIndex) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        String result = template;
        result = result.replace("{{candidateName}}", context.candidateName());
        result = result.replace("{{jobTitle}}", context.jobTitle());
        result = result.replace("{{jobLocation}}", context.jobLocation());
        result = result.replace("{{sequence}}", String.valueOf(sequence));
        result = result.replace("{{repeatIndex}}", String.valueOf(repeatIndex));
        if (repeatIndex > 0 && !result.contains("{{repeatSuffix}}")) {
            result = result + String.format("（拓展问题 %d）", repeatIndex);
        }
        result = result.replace("{{repeatSuffix}}", repeatIndex > 0 ? String.format("（拓展问题 %d）", repeatIndex) : "");
        return result;
    }

    private record TemplateContext(String rawCandidateName,
                                   String rawJobTitle,
                                   String rawJobLocation) {

        private static final String DEFAULT_NAME = "候选人";
        private static final String DEFAULT_TITLE = "目标岗位";
        private static final String DEFAULT_LOCATION = "工作地点";

        String candidateName() {
            return StringUtils.hasText(rawCandidateName) ? rawCandidateName : DEFAULT_NAME;
        }

        String jobTitle() {
            return StringUtils.hasText(rawJobTitle) ? rawJobTitle : DEFAULT_TITLE;
        }

        String jobLocation() {
            return StringUtils.hasText(rawJobLocation) ? rawJobLocation : DEFAULT_LOCATION;
        }
    }

    private record DownloadedFile(byte[] content, String contentType) {
    }
}
