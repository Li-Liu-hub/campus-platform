package com.campushub.order.controller;

import com.campushub.common.response.Result;
import com.campushub.order.dto.OrderCreateRequest;
import com.campushub.order.dto.OrderQueryRequest;
import com.campushub.order.dto.OrderUpdateRequest;
import com.campushub.order.service.OrderService;
import com.campushub.order.vo.OrderPageVO;
import com.campushub.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 订单基础 CRUD 与抢单接口。 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 创建订单，幂等键重复时返回已存在的原订单。 */
    @PostMapping("/create")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.create(request));
    }

    /** 查询我的订单：role 传 sent 查我发布的，received 查我接的。 */
    @GetMapping("/query")
    public Result<OrderPageVO> query(@Valid @ModelAttribute OrderQueryRequest request) {
        return Result.success(orderService.query(request));
    }

    /** 浏览订单：返回订单详情，浏览量加一。 */
    @GetMapping("/get/{orderId}")
    public Result<OrderVO> view(@PathVariable Long orderId) {
        return Result.success(orderService.view(orderId));
    }

    /** 修改订单，仅待接单状态可修改。 */
    @PutMapping("/update/{orderId}")
    public Result<OrderVO> update(@PathVariable Long orderId,
                                  @Valid @RequestBody OrderUpdateRequest request) {
        return Result.success(orderService.update(orderId, request));
    }

    /** 软删除订单，仅待接单状态可删除。 */
    @DeleteMapping("/delete/{orderId}")
    public Result<Void> delete(@PathVariable Long orderId) {
        orderService.delete(orderId);
        return Result.success();
    }

    /** 抢单：分布式锁串行化竞争，数据库状态条件更新兜底，仅一人成功。 */
    @PostMapping("/grab/{orderId}")
    public Result<OrderVO> grab(@PathVariable Long orderId) {
        return Result.success(orderService.grab(orderId));
    }
}
