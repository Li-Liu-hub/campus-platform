package com.campushub.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.shop.entity.UserProduct;
import org.apache.ibatis.annotations.Mapper;

/** 用户购买商品表数据访问接口。 */
@Mapper
public interface UserProductMapper extends BaseMapper<UserProduct> {
}
