package com.campushub.infrastructure.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.storage.ObjectStorageService;
import com.campushub.infrastructure.storage.UploadCredential;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 功能：阿里云 OSS 对象存储实现（唯一实现）——签发前端直传凭证，
 * 文件由浏览器直传 OSS、不经过应用服务器（省带宽、省线程、多图可并发）。
 *
 * <p>凭据安全：AccessKey 仅经环境变量（ALIYUN_OSS_*）注入，仓库配置文件只留占位符，
 * 配置缺失时启动即快速失败并给出明确指引；OSSClient 线程安全，单例共享、销毁时关闭。
 */
@Slf4j
@Service
public class AliyunOssStorageServiceImpl implements ObjectStorageService {

    /** 允许的图片扩展名白名单。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 允许的业务类型白名单：同时是对象键前缀与凭证限制的 key 前缀。 */
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("post", "order", "misc");

    /** 单文件大小上限（字节）：作为凭证 policy 的 content-length-range 限制。 */
    private static final long MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

    /** OSS 客户端（线程安全，单例共享）。 */
    private final OSS ossClient;

    /** 目标存储空间名称。 */
    private final String bucket;

    /** AccessKey ID：PostObject 直传表单需要携带（配合 policy 签名双重限制，非机密字段）。 */
    private final String accessKeyId;

    /** 访问 URL 前缀（不带结尾斜杠）。 */
    private final String urlPrefix;

    /** 直传凭证有效期（秒）：短有效期缩小凭证被滥用的窗口。 */
    private final long credentialExpireSeconds;

    public AliyunOssStorageServiceImpl(
            @Value("${campushub.storage.oss.endpoint:}") String endpoint,
            @Value("${campushub.storage.oss.bucket:}") String bucket,
            @Value("${campushub.storage.oss.access-key-id:}") String accessKeyId,
            @Value("${campushub.storage.oss.access-key-secret:}") String accessKeySecret,
            @Value("${campushub.storage.oss.url-prefix:}") String urlPrefix,
            @Value("${campushub.storage.oss.upload-credential-expire-seconds:60}") long credentialExpireSeconds) {
        requireText(endpoint, "endpoint");
        requireText(bucket, "bucket");
        requireText(accessKeyId, "access-key-id");
        requireText(accessKeySecret, "access-key-secret");
        this.bucket = bucket;
        this.accessKeyId = accessKeyId;
        this.urlPrefix = resolveUrlPrefix(urlPrefix, bucket, endpoint);
        this.credentialExpireSeconds = credentialExpireSeconds;
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("阿里云 OSS 存储已启用，endpoint={}，bucket={}", endpoint, bucket);
    }

    /**
     * 功能：签发 PostObject 直传凭证。
     *
     * <p>policy 三重限制：有效期仅 {@code credentialExpireSeconds} 秒、文件大小 0~5MB、
     * 对象键前缀必须是服务端生成的业务类型前缀——即使凭证泄露，也只能在极短窗口内
     * 往指定前缀传指定大小的图片；对象键由服务端生成，前端无法换 key。
     */
    @Override
    public UploadCredential generateUploadCredential(String originalFilename, String bizType) {
        String type = resolveBizType(bizType);
        String extension = resolveExtension(originalFilename);
        String objectKey = buildObjectKey(type, extension);
        Date expiration = new Date(System.currentTimeMillis() + credentialExpireSeconds * 1000L);
        PolicyConditions conditions = new PolicyConditions();
        conditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, MAX_UPLOAD_BYTES);
        conditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, type + "/");
        String postPolicy = ossClient.generatePostPolicy(expiration, conditions);
        Map<String, String> formFields = new LinkedHashMap<>();
        formFields.put("policy", BinaryUtil.toBase64String(postPolicy.getBytes(StandardCharsets.UTF_8)));
        formFields.put("OSSAccessKeyId", accessKeyId);
        formFields.put("signature", ossClient.calculatePostSignature(postPolicy));
        return new UploadCredential(urlPrefix, objectKey, urlPrefix + "/" + objectKey,
                expiration.getTime() / 1000, formFields);
    }

    /** 应用关闭时释放 OSS 客户端连接。 */
    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云 OSS 客户端已关闭");
        }
    }

    /** 校验业务类型白名单，不通过即拒绝。 */
    private String resolveBizType(String bizType) {
        String type = bizType == null ? "misc" : bizType.trim().toLowerCase();
        if (!ALLOWED_BIZ_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的业务类型");
        }
        return type;
    }

    /** 从原始文件名提取扩展名并校验白名单，不通过即拒绝。 */
    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件缺少扩展名");
        }
        String extension = originalFilename.substring(dot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 jpg/jpeg/png/gif/webp 图片");
        }
        return extension;
    }

    /** 生成对象键：业务类型/日期/随机名.扩展名。 */
    private String buildObjectKey(String type, String extension) {
        return type + "/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    /** 未显式配置 url-prefix 时按 https://{bucket}.{endpoint} 拼接访问前缀。 */
    private static String resolveUrlPrefix(String urlPrefix, String bucket, String endpoint) {
        if (urlPrefix != null && !urlPrefix.isBlank()) {
            return urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        }
        return "https://" + bucket + "." + endpoint;
    }

    /** 配置缺失时快速失败并给出明确指引，避免签发时才暴露问题。 */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "未配置阿里云 OSS 参数：campushub.storage.oss." + name + " 不能为空（请用环境变量 ALIYUN_OSS_* 注入）");
        }
    }
}
