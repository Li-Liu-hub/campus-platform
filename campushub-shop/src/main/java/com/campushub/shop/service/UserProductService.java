package com.campushub.shop.service;

import com.campushub.shop.dto.UserProductCreateRequest;
import com.campushub.shop.dto.UserProductQueryRequest;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.UserProductVO;

/** 用户购买商品基础业务接口。 */
public interface UserProductService {

    /** 下单：校验商品在售并扣减库存后创建购买记录。 */
    UserProductVO create(UserProductCreateRequest request);

    /** 根据记录 ID 查询购买记录。 */
    UserProductVO getById(Long userProductId);

    /** 游标分页查询当前登录用户自己的购买记录。 */
    PageVO<UserProductVO> query(UserProductQueryRequest request);

    /** 物理删除当前登录用户自己的购买记录。 */
    void delete(Long userProductId);
}
