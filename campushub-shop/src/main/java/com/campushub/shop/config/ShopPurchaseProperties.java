package com.campushub.shop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 店铺域购买记录相关配置，集中承载超时关闭任务与接口层共用的取值。
 *
 * <p>超时时长同时被两处使用：定时任务据此算出扫描截止时刻，卖家查询接口据此算出每条记录的
 * 超时时刻。集中在一处是为了避免同一配置项在两个类里各写一份默认值，改动时漏改其一。
 */
@Data
@Component
@ConfigurationProperties(prefix = "campushub.shop")
public class ShopPurchaseProperties {

    /** 待付款购买记录的存活时长（分钟），超过该时长由定时任务关闭并回补库存。 */
    private long orderTimeoutMinutes = 15;

    /** 超时扫描单批最多处理的记录数，避免一次锁定过多行。 */
    private int timeoutBatchSize = 200;
}
