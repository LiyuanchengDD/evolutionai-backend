package com.example.grpcdemo.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Wrapper around the Supabase storage REST API.
 */
@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final SupabaseStorageProperties properties;
    private final WebClient client;
    private final Clock clock;

    public SupabaseStorageService(WebClient.Builder builder,
                                  SupabaseStorageProperties properties,
                                  Clock clock) {
        this.properties = properties;
        this.clock = clock;
        URI baseUrl = properties.resolveApiBaseUrl();
        WebClient.Builder configured = builder
                .baseUrl(baseUrl.toString())
                .filter(logResponse());
        if (StringUtils.hasText(properties.getServiceRoleKey())) {
            configured.defaultHeaders(headers -> {
                headers.setBearerAuth(properties.getServiceRoleKey());
                headers.set("apikey", properties.getServiceRoleKey());
            });
        }
        this.client = configured.build();
    }

    public StorageObjectPointer upload(StorageCategory category,
                                       String originalFileName,
                                       byte[] data,
                                       String contentType) {
        if (data == null || data.length == 0) {
            throw new StorageException("上传内容不能为空");
        }
        SupabaseStorageProperties.Bucket bucketConfig = resolveBucket(category);
        String path = buildObjectPath(bucketConfig, originalFileName);
        MediaType mediaType = parseMediaType(contentType);
        try {
            client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/object/{bucket}/{path}")
                            .build(bucketConfig.getName(), path))
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                    .bodyValue(data)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw new StorageException("上传文件到 Supabase 失败", e);
        }
        return new StorageObjectPointer(bucketConfig.getName(), path, (long) data.length, mediaType.toString());
    }

    public byte[] download(StorageObjectPointer pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return new byte[0];
        }
        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/object/{bucket}/{path}")
                            .build(pointer.bucket(), pointer.path()))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .blockOptional()
                    .orElse(new byte[0]);
        } catch (Exception e) {
            throw new StorageException("从 Supabase 下载文件失败", e);
        }
    }

    public void delete(StorageObjectPointer pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return;
        }
        try {
            client.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/object/{bucket}/{path}")
                            .build(pointer.bucket(), pointer.path()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("删除 Supabase 对象失败 bucket={}, path={}: {}", pointer.bucket(), pointer.path(), e.getMessage());
        }
    }

    public String buildPublicUrl(StorageObjectPointer pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return null;
        }
        URI publicBase = properties.getStorage().getPublicUrl();
        URI apiBase = properties.resolveApiBaseUrl();
        String base = publicBase != null ? publicBase.toString() : apiBase.toString() + "/object/public";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return String.format(Locale.ROOT, "%s/%s/%s", base, pointer.bucket(), pointer.path());
    }

    private SupabaseStorageProperties.Bucket resolveBucket(StorageCategory category) {
        return switch (category) {
            case JOB_DOCUMENT -> properties.getStorage().getJobDocuments();
            case RESUME -> properties.getStorage().getResumes();
            case INTERVIEW_AUDIO -> properties.getStorage().getInterviewAudios();
            case PROFILE_PHOTO -> properties.getStorage().getProfilePhotos();
        };
    }

    private String buildObjectPath(SupabaseStorageProperties.Bucket bucket, String originalFileName) {
        String prefix = bucket.getPathPrefix();
        if (!StringUtils.hasText(prefix)) {
            prefix = "uploads";
        }
        String sanitized = sanitizeFileName(originalFileName);
        if (!StringUtils.hasText(sanitized)) {
            sanitized = UUID.randomUUID().toString();
        }
        Instant now = clock.instant();
        return String.format(Locale.ROOT, "%s/%d-%s", prefix, now.toEpochMilli(), sanitized);
    }

    private String sanitizeFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return null;
        }
        String normalized = Normalizer.normalize(originalFileName, Normalizer.Form.NFKD)
                .replaceAll("[^\\w\\.\-]+", "-")
                .replaceAll("-+", "-");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private MediaType parseMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private ExchangeFilterFunction logResponse() {
        return (request, next) -> next.exchange(request)
                .flatMap(response -> logError(response));
    }

    private Mono<ClientResponse> logError(ClientResponse response) {
        if (response.statusCode().isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        log.error("Supabase storage API 调用失败 status={}, body={}", response.statusCode(), body);
                        return Mono.error(new StorageException("Supabase 存储接口调用失败，状态码: " + response.statusCode()));
                    });
        }
        return Mono.just(response);
    }
}

