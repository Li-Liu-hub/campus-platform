package com.campushub.order.service;

import com.campushub.order.dto.OrderCreateRequest;
import com.campushub.order.dto.OrderQueryRequest;
import com.campushub.order.vo.OrderPageVO;
import com.campushub.order.vo.OrderVO;

/** 订单基础业务接口。 */
public interface OrderService {

    /** 创建当前登录用户的订单，初始状态为待支付。 */
    OrderVO create(OrderCreateRequest request);

    /** 按条件游标分页查询未删除订单。 */
    OrderPageVO query(OrderQueryRequest request);
}
