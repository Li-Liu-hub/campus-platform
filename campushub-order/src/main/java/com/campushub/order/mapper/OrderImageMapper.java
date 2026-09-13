package com.campushub.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.order.entity.OrderImage;
import org.apache.ibatis.annotations.Mapper;

/** 订单图片表数据访问接口。 */
@Mapper
public interface OrderImageMapper extends BaseMapper<OrderImage> {
}
