package com.campushub.infrastructure.storage.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.storage.AbstractObjectStorageService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * 功能：阿里云 OSS 对象存储实现，存储后端切到 OSS 时启用（campushub.storage.type=oss）。
 *
 * <p>凭据安全：AccessKey 仅经环境变量（ALIYUN_OSS_*）注入，仓库配置文件只留占位符，
 * 严禁把凭据写入任何会入库的文件。OSSClient 为线程安全的长连接客户端，
 * 应用生命周期内单例共享，销毁时关闭；上传失败转换为 500 业务异常、
 * 不向上暴露 OSS 的内部错误信息。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "campushub.storage.type", havingValue = "oss")
public class AliyunOssStorageServiceImpl extends AbstractObjectStorageService {

    /** OSS 客户端（线程安全，单例共享）。 */
    private final OSS ossClient;

    /** 目标存储空间名称。 */
    private final String bucket;

    public AliyunOssStorageServiceImpl(
            @Value("${campushub.storage.oss.endpoint:}") String endpoint,
            @Value("${campushub.storage.oss.bucket:}") String bucket,
            @Value("${campushub.storage.oss.access-key-id:}") String accessKeyId,
            @Value("${campushub.storage.oss.access-key-secret:}") String accessKeySecret,
            @Value("${campushub.storage.oss.url-prefix:}") String urlPrefix) {
        super(resolveUrlPrefix(urlPrefix, bucket, endpoint));
        requireText(endpoint, "endpoint");
        requireText(bucket, "bucket");
        requireText(accessKeyId, "access-key-id");
        requireText(accessKeySecret, "access-key-secret");
        this.bucket = bucket;
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("阿里云 OSS 存储已启用，endpoint={}，bucket={}", endpoint, bucket);
    }

    /** 把内容写入 OSS 对象，失败时仅记日志并转换异常，不回传 OSS 内部错误细节。 */
    @Override
    protected void store(String objectKey, byte[] content) {
        try {
            ossClient.putObject(bucket, objectKey, new ByteArrayInputStream(content));
        } catch (Exception exception) {
            log.error("图片上传 OSS 失败，bucket={}，objectKey={}", bucket, objectKey, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /** 应用关闭时释放 OSS 客户端连接。 */
    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云 OSS 客户端已关闭");
        }
    }

    /** 未显式配置 url-prefix 时按 https://{bucket}.{endpoint} 拼接访问前缀。 */
    private static String resolveUrlPrefix(String urlPrefix, String bucket, String endpoint) {
        if (urlPrefix != null && !urlPrefix.isBlank()) {
            return urlPrefix;
        }
        return "https://" + bucket + "." + endpoint;
    }

    /** 配置缺失时快速失败并给出明确指引，避免上传时才暴露问题。 */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "存储后端为 oss 时 campushub.storage.oss." + name + " 不能为空（请用环境变量 ALIYUN_OSS_* 注入）");
        }
    }
}
