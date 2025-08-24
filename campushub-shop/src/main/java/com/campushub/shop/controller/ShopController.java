package com.campushub.shop.controller;

import com.campushub.common.response.Result;
import com.campushub.shop.dto.ShopCreateRequest;
import com.campushub.shop.dto.ShopQueryRequest;
import com.campushub.shop.dto.ShopUpdateRequest;
import com.campushub.shop.service.ShopService;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.ShopVO;
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

/** 店铺基础 CRUD 接口。 */
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /** 创建店铺。 */
    @PostMapping("/create")
    public Result<ShopVO> create(@Valid @RequestBody ShopCreateRequest request) {
        return Result.success(shopService.create(request));
    }

    /** 按条件游标分页查询店铺，翻页时传上一页返回的游标二元组。 */
    @GetMapping("/query")
    public Result<PageVO<ShopVO>> query(@Valid @ModelAttribute ShopQueryRequest request) {
        return Result.success(shopService.query(request));
    }

    /** 查询店铺详情。 */
    @GetMapping("/get/{shopId}")
    public Result<ShopVO> getById(@PathVariable Long shopId) {
        return Result.success(shopService.getById(shopId));
    }

    /** 修改店铺。 */
    @PutMapping("/update/{shopId}")
    public Result<ShopVO> update(@PathVariable Long shopId,
                                 @Valid @RequestBody ShopUpdateRequest request) {
        return Result.success(shopService.update(shopId, request));
    }

    /** 软删除店铺。 */
    @DeleteMapping("/delete/{shopId}")
    public Result<Void> delete(@PathVariable Long shopId) {
        shopService.delete(shopId);
        return Result.success();
    }
}
