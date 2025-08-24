package com.campushub.shop.service;

import com.campushub.shop.dto.ShopCreateRequest;
import com.campushub.shop.dto.ShopQueryRequest;
import com.campushub.shop.dto.ShopUpdateRequest;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.ShopVO;

/** 店铺基础业务接口。 */
public interface ShopService {

    /** 创建店铺。 */
    ShopVO create(ShopCreateRequest request);

    /** 根据店铺 ID 查询未删除店铺。 */
    ShopVO getById(Long shopId);

    /** 按条件游标分页查询未删除店铺。 */
    PageVO<ShopVO> query(ShopQueryRequest request);

    /** 修改店铺。 */
    ShopVO update(Long shopId, ShopUpdateRequest request);

    /** 软删除店铺。 */
    void delete(Long shopId);
}
