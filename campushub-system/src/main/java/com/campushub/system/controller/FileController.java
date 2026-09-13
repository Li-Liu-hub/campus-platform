package com.campushub.system.controller;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.response.Result;
import com.campushub.infrastructure.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传接口（通用能力，归属系统模块）。
 *
 * <p>当前存储后端为本地磁盘实现，返回的 URL 由 web 层静态资源映射对外提供访问；
 * 将来切换 OSS 时 URL 体系不变，调用方（帖子/订单/消息）无感。
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final ObjectStorageService objectStorageService;

    /**
     * 功能：上传图片，返回可直接访问的 URL。
     *
     * @param file multipart 文件，仅支持 jpg/jpeg/png/gif/webp，大小上限见 multipart 配置
     * @param bizType 业务类型（post/order/misc），决定存储路径分段
     * @return 图片访问 URL
     * @throws IOException 读取上传内容失败时抛出
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "bizType", defaultValue = "misc") String bizType)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        return Result.success(objectStorageService.upload(file.getBytes(), file.getOriginalFilename(), bizType));
    }
}
