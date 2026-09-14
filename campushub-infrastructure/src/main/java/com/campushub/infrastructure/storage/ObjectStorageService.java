package com.campushub.infrastructure.storage;

/**
 * 功能：对象存储能力接口——签发前端直传凭证，文件由浏览器直传对象存储、不经过应用服务器。
 *
 * <p>业务侧只持有上传后的 URL，不感知存储位置；当前唯一实现为阿里云 OSS
 * （本地磁盘模式已按“全部直传”的方向移除）。
 */
public interface ObjectStorageService {

    /**
     * 功能：签发前端直传凭证。
     *
     * <p>服务端职责：校验文件名与业务类型白名单、生成唯一对象键、签发带限制的临时凭证
     * （大小上限、对象键前缀、有效期）；前端职责：以 multipart/form-data 把文件与
     * {@link UploadCredential#formFields()} POST 到 {@link UploadCredential#uploadHost()}，
     * 成功后使用 {@link UploadCredential#accessUrl()}。
     *
     * @param originalFilename 原始文件名，用于提取扩展名做图片类型校验
     * @param bizType 业务类型（post/order/misc），作为对象键前缀（同时也是凭证限制的 key 前缀）
     * @return 直传凭证（上传域名、对象键、签名字段、过期时刻与访问 URL）
     * @throws com.campushub.common.exception.BusinessException 文件名或业务类型不合法时抛出
     */
    UploadCredential generateUploadCredential(String originalFilename, String bizType);
}
