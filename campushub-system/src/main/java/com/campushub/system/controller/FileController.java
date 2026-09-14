package com.campushub.system.controller;

import com.campushub.common.response.Result;
import com.campushub.infrastructure.storage.ObjectStorageService;
import com.campushub.infrastructure.storage.UploadCredential;
import com.campushub.system.dto.UploadCredentialRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件上传接口（通用能力，归属系统模块）。
 *
 * <p>采用前端直传模式：本接口只签发带限制的上传凭证（对象键、签名、有效期），
 * 文件由浏览器直传对象存储，不经过应用服务器——省带宽、省线程、多图可并发上传。
 * 服务端中转（multipart）路径已移除；本地存储后端不支持直传，调用本接口会得到明确的业务提示。
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final ObjectStorageService objectStorageService;

    /**
     * 功能：签发前端直传凭证。
     *
     * @param request 原始文件名（用于扩展名白名单校验）与业务类型（post/order/misc）
     * @return 直传凭证：上传域名、对象键、签名字段、过期时刻与上传完成后的访问 URL
     */
    @PostMapping("/upload-credential")
    public Result<UploadCredential> uploadCredential(@Valid @RequestBody UploadCredentialRequest request) {
        return Result.success(objectStorageService.generateUploadCredential(
                request.fileName(), request.bizType()));
    }
}
