package com.campushub.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改店铺请求参数。 */
public record ShopUpdateRequest(
        @NotBlank(message = "店铺名称不能为空")
        @Size(max = 64, message = "店铺名称长度不能超过 64 个字符")
        String shopName,

        @Size(max = 255, message = "店铺简介长度不能超过 255 个字符")
        String shopDescription
) {
}
