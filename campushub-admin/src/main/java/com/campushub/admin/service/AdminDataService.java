package com.campushub.admin.service;

import com.campushub.admin.dto.AdminPageQueryRequest;
import com.campushub.admin.vo.AdminPostVO;
import com.campushub.admin.vo.AdminProductVO;
import com.campushub.admin.vo.AdminShopVO;
import com.campushub.admin.vo.AdminUserVO;
import com.campushub.admin.vo.PageVO;

/**
 * 管理端数据查看业务接口。
 *
 * <p>所有接口仅管理员可访问：管理员校验在实现内统一完成，非管理员一律 403。
 */
public interface AdminDataService {

    /** 分页查看用户数据，按注册时间倒序。 */
    PageVO<AdminUserVO> listUsers(AdminPageQueryRequest request);

    /** 分页查看帖子数据（含发帖人昵称），按发布时间倒序。 */
    PageVO<AdminPostVO> listPosts(AdminPageQueryRequest request);

    /** 分页查看店铺数据（含店主昵称），按创建时间倒序。 */
    PageVO<AdminShopVO> listShops(AdminPageQueryRequest request);

    /** 分页查看商品数据（含所属店铺名称），按创建时间倒序。 */
    PageVO<AdminProductVO> listProducts(AdminPageQueryRequest request);
}
