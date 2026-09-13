package com.campushub.infrastructure.storage;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 功能：对象存储实现的公共骨架——统一收口「业务类型/扩展名白名单校验、对象键生成、URL 拼接」，
 * 子类只负责把内容写入各自的介质（本地磁盘 / 阿里云 OSS）。
 *
 * <p>安全边界：业务类型与扩展名都走白名单校验——业务类型参与对象键拼接，若不校验
 * 可被 {@code ../} 路径注入；扩展名白名单拒绝非图片内容。
 */
@Slf4j
public abstract class AbstractObjectStorageService implements ObjectStorageService {

    /** 允许的图片扩展名白名单。 */
    protected static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 允许的业务类型白名单：同时是对象键的路径分段，必须先于拼接校验。 */
    protected static final Set<String> ALLOWED_BIZ_TYPES = Set.of("post", "order", "misc");

    /** URL 前缀（不带结尾斜杠）。 */
    private final String urlPrefix;

    protected AbstractObjectStorageService(String urlPrefix) {
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    /** 上传：校验入参并生成对象键后交给子类写入介质，返回「前缀/对象键」形式的访问 URL。 */
    @Override
    public String upload(byte[] content, String originalFilename, String bizType) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传内容不能为空");
        }
        String type = resolveBizType(bizType);
        String extension = resolveExtension(originalFilename);
        String objectKey = buildObjectKey(type, extension);
        store(objectKey, content);
        String url = urlPrefix + "/" + objectKey;
        log.info("图片上传成功，url={}，size={}B", url, content.length);
        return url;
    }

    /**
     * 功能：把内容写入存储介质。
     *
     * @param objectKey 对象键（业务类型/日期/随机名.扩展名）
     * @param content 文件字节内容，非空
     * @throws BusinessException 写入失败时抛出，由实现记日志后转换
     */
    protected abstract void store(String objectKey, byte[] content);

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
}
