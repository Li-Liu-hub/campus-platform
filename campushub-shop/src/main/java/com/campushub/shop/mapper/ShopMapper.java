package com.campushub.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.shop.entity.Shop;
import org.apache.ibatis.annotations.Mapper;

/** 店铺表数据访问接口。 */
@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
}
