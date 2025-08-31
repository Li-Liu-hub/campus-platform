package com.campushub.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 商品表数据访问接口。 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 条件扣减库存：库存充足才扣，返回 0 表示并发下已被抢空。 */
    int deductStock(@Param("productId") Long productId, @Param("number") long number);

    /** 增量调整库存：正数补货、负数减库，减少量超过当前库存导致结果小于 0 时返回 0 且不改动。 */
    int adjustStock(@Param("productId") Long productId, @Param("delta") long delta);
}
