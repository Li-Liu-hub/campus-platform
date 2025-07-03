package com.campushub.order.controller;

import com.campushub.common.response.Result;
import com.campushub.order.dto.OrderCreateRequest;
import com.campushub.order.dto.OrderQueryRequest;
import com.campushub.order.service.OrderService;
import com.campushub.order.vo.OrderPageVO;
import com.campushub.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 订单基础接口。 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 创建订单。 */
    @PostMapping("/create")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.create(request));
    }

    /** 按条件游标分页查询订单，翻页时传上一页返回的游标二元组。 */
    @GetMapping("/query")
    public Result<OrderPageVO> query(@Valid @ModelAttribute OrderQueryRequest request) {
        return Result.success(orderService.query(request));
    }
}
