package com.campushub.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理端店铺列表行，含店主昵称（SQL 直查组装，不依赖店铺模块代码）。 */
@Data
public class AdminShopVO {

    /** 店铺 ID。 */
    private Long shopId;

    /** 店铺名称。 */
    private String shopName;

    /** 店主用户 ID。 */
    private Long shopUserId;

    /** 店主昵称，店主已注销时为空。 */
    private String shopUserNickname;

    /** 店铺简介。 */
    private String shopDescription;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
