package com.campushub.shop.service;

import com.campushub.shop.dto.PendingPurchaseQueryRequest;
import com.campushub.shop.dto.ProductCreateRequest;
import com.campushub.shop.dto.ProductQueryRequest;
import com.campushub.shop.dto.ProductUpdateRequest;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.PendingPurchaseVO;
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

    /** 软删除商品，存在待付款购买记录时拒绝删除。 */
    void delete(Long productId);

    /** 卖家查看某商品当前待付款的购买记录，仅限该商品所属店铺的店主。 */
    PageVO<PendingPurchaseVO> listPendingPurchases(Long productId, PendingPurchaseQueryRequest request);
}
