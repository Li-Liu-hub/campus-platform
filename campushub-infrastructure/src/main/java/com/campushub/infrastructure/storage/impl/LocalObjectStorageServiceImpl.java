package com.campushub.infrastructure.storage.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.storage.ObjectStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 功能：本地磁盘对象存储实现，文件落到「根目录/业务类型/日期/uuid.扩展名」，URL 为「前缀/对象键」。
 *
 * <p>安全边界：业务类型与扩展名都走白名单校验——业务类型参与路径拼接，若不校验
 * 可被 {@code ../} 路径注入；扩展名白名单拒绝非图片内容。存储目录与 URL 前缀
 * 由配置提供（campushub.storage.local.*），URL 访问由 web 层的静态资源映射承接。
 */
@Slf4j
@Service
public class LocalObjectStorageServiceImpl implements ObjectStorageService {

    /** 允许的图片扩展名白名单。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 允许的业务类型白名单：同时是路径分段，必须先于拼接校验。 */
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("post", "order", "misc");

    /** 存储根目录的绝对路径。 */
    private final String rootDir;

    /** URL 前缀（不带结尾斜杠）。 */
    private final String urlPrefix;

    public LocalObjectStorageServiceImpl(
            @Value("${campushub.storage.local.root-dir:./uploads}") String rootDir,
            @Value("${campushub.storage.local.url-prefix:/files}") String urlPrefix) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize().toString();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    /** 上传图片：校验业务类型与扩展名后按「业务/日期/随机名」落盘，返回访问 URL。 */
    @Override
    public String upload(byte[] content, String originalFilename, String bizType) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传内容不能为空");
        }
        String type = bizType == null ? "misc" : bizType.trim().toLowerCase();
        if (!ALLOWED_BIZ_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的业务类型");
        }
        String extension = resolveExtension(originalFilename);
        String objectKey = type + "/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Path target = Paths.get(rootDir, objectKey);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException exception) {
            log.error("图片写入本地磁盘失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        String url = urlPrefix + "/" + objectKey;
        log.info("图片上传成功，url={}，size={}B", url, content.length);
        return url;
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
}
