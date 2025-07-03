package com.campushub.order.service.impl;

import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.util.StringUtils;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.order.dto.OrderCreateRequest;
import com.campushub.order.dto.OrderQueryRequest;
import com.campushub.order.entity.Order;
import com.campushub.order.mapper.OrderMapper;
import com.campushub.order.service.OrderService;
import com.campushub.order.vo.OrderPageVO;
import com.campushub.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 订单基础业务实现。 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 订单初始状态：待支付。 */
    private static final int STATUS_PENDING_PAYMENT = 0;

    private final OrderMapper orderMapper;

    private final AuthenticationService authenticationService;

    private final StringUtils stringUtils;

    /** 创建当前登录用户的订单，初始状态为待支付。 */
    @Override
    public OrderVO create(OrderCreateRequest request) {
        Long userId = requireCurrentUserId();
        Order order = new Order();
        order.setOrderUserId(userId);
        order.setOrderTitle(request.orderTitle().trim());
        order.setOrderAmount(request.orderAmount());
        order.setOrderStatus(STATUS_PENDING_PAYMENT);
        order.setOrderIsDelete(0);
        orderMapper.insert(order);
        return toOrderVO(orderMapper.selectById(order.getOrderId()));
    }

    /** 按条件游标分页查询订单，返回本页数据和下一页游标，默认按创建时间倒序。 */
    @Override
    public OrderPageVO query(OrderQueryRequest request) {
        boolean orderByAsc = "asc".equalsIgnoreCase(stringUtils.normalizeText(request.sortOrder()));
        int pageSize = resolvePageSize(request.pageSize());

        List<Order> orders = orderMapper.selectByCondition(request.orderStatus(), orderByAsc,
                request.cursorTime(), request.cursorId(), pageSize);
        List<OrderVO> list = orders.stream()
                .map(this::toOrderVO)
                .toList();
        if (list.size() < pageSize) {
            return new OrderPageVO(list, false, null, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        OrderVO lastOrder = list.get(list.size() - 1);
        return new OrderPageVO(list, true, lastOrder.createTime(), lastOrder.orderId());
    }

    /** 页大小缺省 20，并限制在 1 到 100 之间。 */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }

    /** 将订单实体转换为接口返回对象。 */
    private OrderVO toOrderVO(Order order) {
        return new OrderVO(
                order.getOrderId(),
                order.getOrderUserId(),
                order.getOrderTitle(),
                order.getOrderAmount(),
                order.getOrderStatus(),
                order.getCreateTime(),
                order.getUpdateTime()
        );
    }
}
