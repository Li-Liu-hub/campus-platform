package com.campushub.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 订单表数据访问接口。 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 功能：按角色与状态查询当前用户的订单，结果按创建时间倒序。
     *
     * @param role 查询角色：sent 查我发布的，received 查我接的
     * @param userId 当前登录用户 ID
     * @param orderStatus 订单状态过滤，null 表示不过滤
     * @param pageSize 返回行数上限
     * @return 最多 pageSize 条订单
     */
    List<Order> selectByCondition(@Param("role") String role,
                                  @Param("userId") Long userId,
                                  @Param("orderStatus") Integer orderStatus,
                                  @Param("pageSize") int pageSize);

    /**
     * 功能：抢单条件更新（CAS 兜底），仅当订单仍处于待接单状态时回填接单人并流转状态。
     *
     * @param orderId 订单主键
     * @param receiveUserId 抢单用户 ID
     * @return 实际更新行数：1 表示抢单成功，0 表示订单已被他人抢走或已关闭
     */
    int casGrab(@Param("orderId") Long orderId, @Param("receiveUserId") Long receiveUserId);

    /** 将指定未删除订单的浏览量原子加一，并发下不会丢失计数。 */
    int increaseViewNumber(@Param("orderId") Long orderId);
}
