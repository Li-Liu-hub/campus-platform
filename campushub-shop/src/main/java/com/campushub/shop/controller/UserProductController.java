package com.campushub.shop.controller;

import com.campushub.common.response.Result;
import com.campushub.shop.dto.UserProductCreateRequest;
import com.campushub.shop.dto.UserProductQueryRequest;
import com.campushub.shop.service.UserProductService;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.UserProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户购买商品基础接口。 */
@RestController
@RequestMapping("/api/v1/user-products")
@RequiredArgsConstructor
public class UserProductController {

    private final UserProductService userProductService;

    /** 下单：校验商品在售并扣减库存后创建购买记录。 */
    @PostMapping("/create")
    public Result<UserProductVO> create(@Valid @RequestBody UserProductCreateRequest request) {
        return Result.success(userProductService.create(request));
    }

    /** 游标分页查询当前登录用户自己的购买记录，翻页时传上一页返回的游标二元组。 */
    @GetMapping("/query")
    public Result<PageVO<UserProductVO>> query(@Valid @ModelAttribute UserProductQueryRequest request) {
        return Result.success(userProductService.query(request));
    }

    /** 查询购买记录详情。 */
    @GetMapping("/get/{userProductId}")
    public Result<UserProductVO> getById(@PathVariable Long userProductId) {
        return Result.success(userProductService.getById(userProductId));
    }

    /** 物理删除当前登录用户自己的购买记录。 */
    @DeleteMapping("/delete/{userProductId}")
    public Result<Void> delete(@PathVariable Long userProductId) {
        userProductService.delete(userProductId);
        return Result.success();
    }
}
