package com.campushub.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.shop.entity.UserProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 用户购买商品表数据访问接口。 */
@Mapper
public interface UserProductMapper extends BaseMapper<UserProduct> {

    /**
     * 功能：买家付款，把待付款记录抢占为已付款并回填付款时间。
     *
     * <p>状态条件是并发胜负的唯一判据：与超时任务同时发生时，只有一方能把
     * order_status 从 0 改走，另一方影响行数为 0。**必须抢到状态才允许动库存。**
     *
     * <p>付款时间由调用方以应用时钟传入，而不是用 MySQL 的 NOW()：同一行的
     * purchase_time 已由应用时钟写入，若付款时间改用数据库时钟，两侧时区不一致时
     * 同一笔记录会出现"付款早于下单"的荒谬时间。
     *
     * @param userProductId 购买记录 ID
     * @param userId 当前登录买家 ID，用于归属校验，非本人记录影响 0 行
     * @param paymentTime 付款时刻，由应用时钟生成
     * @return 影响行数，0 表示记录不属本人、已被取消或已被超时任务关闭
     */
    int markPaidIfPending(@Param("userProductId") Long userProductId,
                          @Param("userId") Long userId,
                          @Param("paymentTime") LocalDateTime paymentTime);

    /**
     * 功能：买家主动取消，把待付款记录抢占为已取消。
     *
     * <p>只能取消自己的、且仍是待付款的记录；已付款的不允许自行取消。
     *
     * @param userProductId 购买记录 ID
     * @param userId 当前登录买家 ID，用于归属校验
     * @return 影响行数，0 表示记录不属本人或已非待付款状态
     */
    int markCancelledIfPendingByUser(@Param("userProductId") Long userProductId, @Param("userId") Long userId);

    /**
     * 功能：超时任务关闭，把待付款记录抢占为已取消（不校验买家归属）。
     *
     * <p>与买家主动取消共用同一状态抢占条件，谁先抢到谁生效，天然互斥。
     *
     * @param userProductId 购买记录 ID
     * @return 影响行数，0 表示该记录已被付款或已被取消，本轮无需回补库存
     */
    int markCancelledIfPending(@Param("userProductId") Long userProductId);

    /**
     * 功能：查询超时未付款的购买记录，按下单时间升序取一批。
     *
     * <p>比较列刻意用 purchase_time（由应用时钟写入）而不是 create_time（由数据库
     * CURRENT_TIMESTAMP 默认值写入）：deadline 是应用算出来的，两边必须同源时钟。
     * 若改用数据库时钟写入的列，一旦数据库容器时区与 JVM 不一致（如容器 UTC、JVM
     * 东八区），create_time 会比 deadline 早 8 小时，条件恒成立，导致刚下单的订单
     * 在下一轮扫描就被误判超时关闭。
     *
     * <p>走 idx_status_purchase (order_status, purchase_time)，避免全表扫描。
     *
     * @param deadline 超时截止时刻，下单时间早于该时刻的记录视为超时
     * @param limit 单批最多返回条数，避免一次锁定过多行
     * @return 超时候选记录列表，可能为空
     */
    List<UserProduct> selectTimeoutCandidates(@Param("deadline") LocalDateTime deadline,
                                             @Param("limit") int limit);
}
