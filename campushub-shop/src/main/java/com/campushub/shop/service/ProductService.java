package com.campushub.shop.service;

import com.campushub.shop.dto.ProductCreateRequest;
import com.campushub.shop.dto.ProductQueryRequest;
import com.campushub.shop.dto.ProductUpdateRequest;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.ProductVO;

/** 商品基础业务接口。 */
public interface ProductService {

    /** 在指定店铺下创建商品。 */
    ProductVO create(ProductCreateRequest request);

    /** 根据商品 ID 查询未删除商品。 */
    ProductVO getById(Long productId);

    /** 按店铺游标分页查询未删除商品。 */
    PageVO<ProductVO> query(ProductQueryRequest request);

    /** 修改商品。 */
    ProductVO update(Long productId, ProductUpdateRequest request);

    /** 软删除商品。 */
    void delete(Long productId);
}
