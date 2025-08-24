package com.campushub.shop.controller;

import com.campushub.common.response.Result;
import com.campushub.shop.dto.ProductCreateRequest;
import com.campushub.shop.dto.ProductQueryRequest;
import com.campushub.shop.dto.ProductUpdateRequest;
import com.campushub.shop.service.ProductService;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.ProductVO;
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

/** 商品基础 CRUD 接口。 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 创建商品。 */
    @PostMapping("/create")
    public Result<ProductVO> create(@Valid @RequestBody ProductCreateRequest request) {
        return Result.success(productService.create(request));
    }

    /** 按店铺游标分页查询商品，翻页时传上一页返回的游标二元组。 */
    @GetMapping("/query")
    public Result<PageVO<ProductVO>> query(@Valid @ModelAttribute ProductQueryRequest request) {
        return Result.success(productService.query(request));
    }

    /** 查询商品详情。 */
    @GetMapping("/get/{productId}")
    public Result<ProductVO> getById(@PathVariable Long productId) {
        return Result.success(productService.getById(productId));
    }

    /** 修改商品。 */
    @PutMapping("/update/{productId}")
    public Result<ProductVO> update(@PathVariable Long productId,
                                    @Valid @RequestBody ProductUpdateRequest request) {
        return Result.success(productService.update(productId, request));
    }

    /** 软删除商品。 */
    @DeleteMapping("/delete/{productId}")
    public Result<Void> delete(@PathVariable Long productId) {
        productService.delete(productId);
        return Result.success();
    }
}
