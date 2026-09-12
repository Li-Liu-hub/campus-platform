package com.campushub.shop.vo;

import java.time.LocalDateTime;

/**
 * 卖家视角的待付款记录返回对象。
 *
 * <p>刻意只暴露交易必要信息：买家用户 ID、购买数量、下单时间与超时时刻。
 * 不返回买家手机号、昵称等联系方式，卖家确需联系买家应走站内其他渠道。
 */
public record PendingPurchaseVO(
        Long buyerUserId,
        Long productNumber,
        LocalDateTime purchaseTime,
        LocalDateTime expireTime
) {
}
