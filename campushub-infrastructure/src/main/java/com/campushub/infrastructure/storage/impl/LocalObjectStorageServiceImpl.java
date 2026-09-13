package com.campushub.infrastructure.storage.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.storage.AbstractObjectStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 功能：本地磁盘对象存储实现，文件落到「根目录/对象键」，URL 为「前缀/对象键」。
 *
 * <p>存储目录与 URL 前缀由配置提供（campushub.storage.local.*），URL 访问由 web 层的
 * 静态资源映射承接；默认生效（campushub.storage.type 未配置或为 local 时装配），
 * 切到 OSS 时由 {@code campushub.storage.type=oss} 关闭本实现。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "campushub.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageServiceImpl extends AbstractObjectStorageService {

    /** 存储根目录的绝对路径。 */
    private final String rootDir;

    public LocalObjectStorageServiceImpl(
            @Value("${campushub.storage.local.root-dir:./uploads}") String rootDir,
            @Value("${campushub.storage.local.url-prefix:/files}") String urlPrefix) {
        super(urlPrefix);
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize().toString();
    }

    /** 把内容写入本地磁盘，自动创建父目录。 */
    @Override
    protected void store(String objectKey, byte[] content) {
        try {
            Path target = Paths.get(rootDir, objectKey);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException exception) {
            log.error("图片写入本地磁盘失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
