package com.campushub.infrastructure.storage;

import java.util.Map;

/**
 * 前端直传凭证：浏览器据此把文件《直接》上传到对象存储，文件流不经过应用服务器。
 *
 * <p>服务端职责：生成唯一对象键（白名单校验后的路径）、签发带限制的临时凭证
 * （大小上限、对象键前缀、过期时间）；前端职责：以 multipart/form-data 把文件
 * 与 {@code formFields} 一起 POST 到 {@code uploadHost}，成功后使用 {@code accessUrl}。
 *
 * @param uploadHost 直传的 POST 目标（对象存储访问域名）
 * @param objectKey 服务端生成的对象键，作为表单字段 key 的值提交
 * @param accessUrl 上传成功后可直接使用的访问 URL
 * @param expireAtEpochSeconds 凭证过期时刻（Unix 秒），过期后签名失效
 * @param formFields 需要随表单一并提交的签名字段（policy / OSSAccessKeyId / signature）
 */
public record UploadCredential(
        String uploadHost,
        String objectKey,
        String accessUrl,
        long expireAtEpochSeconds,
        Map<String, String> formFields
) {
}
