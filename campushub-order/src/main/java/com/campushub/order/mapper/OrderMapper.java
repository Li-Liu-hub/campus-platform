package com.campushub.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 订单表数据访问接口。 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 功能：按游标分页查询未删除订单，结果按 (create_time, order_id) 排序。
     *
     * @param orderStatus 订单状态，可选
     * @param orderByAsc true 按创建时间正序，false 按创建时间倒序
     * @param cursorTime 上一页末行创建时间，首页传 null
     * @param cursorId 上一页末行订单 ID，与 cursorTime 成对使用，首页传 null
     * @param pageSize 本页行数上限
     * @return 最多 pageSize 条订单
     */
    List<Order> selectByCondition(@Param("orderStatus") Integer orderStatus,
                                  @Param("orderByAsc") boolean orderByAsc,
                                  @Param("cursorTime") LocalDateTime cursorTime,
                                  @Param("cursorId") Long cursorId,
                                  @Param("pageSize") int pageSize);
}
