package com.campushub.order.service;

import com.campushub.order.dto.OrderCreateRequest;
import com.campushub.order.dto.OrderQueryRequest;
import com.campushub.order.dto.OrderUpdateRequest;
import com.campushub.order.vo.OrderPageVO;
import com.campushub.order.vo.OrderVO;

/** 订单基础业务：增删改查与并发抢单。 */
public interface OrderService {

    /**
     * 功能：创建当前登录用户发布的订单，幂等键重复时返回已存在的原订单。
     *
     * @param request 订单创建参数，类型、金额、最晚支付时间与幂等键必填
     * @return 已创建或已存在的订单详情
     */
    OrderVO create(OrderCreateRequest request);

    /**
     * 功能：查询未删除订单详情。
     *
     * @param orderId 订单主键，不允许为空
     * @return 订单详情
     */
    OrderVO getById(Long orderId);

    /**
     * 功能：浏览订单，返回详情并将浏览量原子加一。
     *
     * @param orderId 订单主键，不允许为空
     * @return 订单详情，浏览量为自增前快照，允许短暂滞后
     */
    OrderVO view(Long orderId);

    /**
     * 功能：按角色与状态查询当前登录用户的订单。
     *
     * @param request 查询条件：角色（sent/received）、状态过滤与页大小
     * @return 订单列表，按创建时间倒序
     */
    OrderPageVO query(OrderQueryRequest request);

    /**
     * 功能：修改当前登录用户发布的待接单订单。
     *
     * @param orderId 订单主键，不允许为空
     * @param request 修改参数，字段留空表示不修改
     * @return 修改后的订单详情
     */
    OrderVO update(Long orderId, OrderUpdateRequest request);

    /**
     * 功能：软删除当前登录用户发布的待接单订单。
     *
     * @param orderId 订单主键，不允许为空
     */
    void delete(Long orderId);

    /**
     * 功能：抢单——分布式锁串行化同一订单的抢单请求，数据库状态条件更新兜底，
     * 并发下保证仅一人接单成功。
     *
     * @param orderId 订单主键，不允许为空
     * @return 接单成功后的订单详情
     */
    OrderVO grab(Long orderId);
}
