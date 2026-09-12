package com.campushub.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商品表数据访问接口。 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 功能：冻结库存，把可卖量扣减到冻结量上（一条语句同时改两列，不留中间态）。
     *
     * <p>下单时调用。拆成两条 UPDATE 会留下"可卖已减、冻结未加"的中间态，
     * 此时若用户取消，回补就会凭空多出库存，所以必须写在同一条语句里。
     *
     * @param productId 商品 ID
     * @param number 下单数量
     * @return 影响行数，0 表示商品已下架或可卖量不足（并发下已被抢空）
     */
    int freezeStock(@Param("productId") Long productId, @Param("number") long number);

    /**
     * 功能：消耗冻结量，付款成功时调用。
     *
     * <p>只减冻结量而**不回补可卖量**——货真的卖掉了，这是消耗不是归还，
     * 这一点与解冻回补构成付款与取消的分水岭。
     *
     * @param productId 商品 ID
     * @param number 订单数量
     * @return 影响行数，0 表示冻结量对不上（数据已不一致），调用方应抛异常回滚并告警
     */
    int consumeLockedStock(@Param("productId") Long productId, @Param("number") long number);

    /**
     * 功能：解冻回补，冻结量减、可卖量加（一条语句同时改两列）。
     *
     * <p>买家主动取消或超时任务关闭购买记录时调用，把占用还回可卖池。
     *
     * @param productId 商品 ID
     * @param number 需要归还的数量
     * @return 影响行数，0 表示冻结量对不上（数据已不一致），调用方应抛异常回滚并告警
     */
    int releaseLockedStock(@Param("productId") Long productId, @Param("number") long number);

    /**
     * 功能：增量调整可卖库存，卖家补货或减库时调用。
     *
     * <p>刻意只动可卖量：冻结量是系统占用，卖家能改就会破坏账目一致性。
     *
     * @param productId 商品 ID
     * @param delta 调整量，正数补货、负数减库
     * @return 影响行数，0 表示减少量超过当前可卖量（结果会小于 0）
     */
    int adjustStock(@Param("productId") Long productId, @Param("delta") long delta);
}
