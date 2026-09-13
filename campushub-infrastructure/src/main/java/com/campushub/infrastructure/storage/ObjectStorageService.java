package com.campushub.infrastructure.storage;

/**
 * 功能：对象存储能力接口——上传文件并返回可公开访问的 URL，业务侧只持有 URL、不感知存储位置。
 *
 * <p>当前实现为本地磁盘（路径与 URL 前缀可配置）；引入 OSS 时新增一个实现类
 * 替换本接口即可，业务方（帖子/订单/消息图片）无需改动。
 */
public interface ObjectStorageService {

    /**
     * 功能：上传对象并返回可公开访问的 URL。
     *
     * @param content 文件字节内容，不允许为空
     * @param originalFilename 原始文件名，用于提取扩展名做图片类型校验
     * @param bizType 业务类型（post/order/misc），用于落盘路径分段与归类
     * @return 可公开访问的 URL
     * @throws com.campushub.common.exception.BusinessException 类型不在白名单或写入失败时抛出
     */
    String upload(byte[] content, String originalFilename, String bizType);
}
