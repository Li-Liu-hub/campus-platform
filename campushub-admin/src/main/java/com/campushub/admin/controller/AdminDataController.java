package com.campushub.admin.controller;

import com.campushub.admin.dto.AdminPageQueryRequest;
import com.campushub.admin.service.AdminDataService;
import com.campushub.admin.vo.AdminPostVO;
import com.campushub.admin.vo.AdminProductVO;
import com.campushub.admin.vo.AdminShopVO;
import com.campushub.admin.vo.AdminUserVO;
import com.campushub.admin.vo.PageVO;
import com.campushub.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端数据查看接口：供管理员浏览平台各域数据（用户/帖子/店铺/商品）。
 *
 * <p>仅做查看，不含删改操作（删除动作归各业务域的既有接口）；
 * 管理员校验在服务层统一完成，非管理员访问一律 403。
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDataController {

    private final AdminDataService adminDataService;

    /** 分页查看用户数据。 */
    @GetMapping("/users/list")
    public Result<PageVO<AdminUserVO>> listUsers(@Valid @ModelAttribute AdminPageQueryRequest request) {
        return Result.success(adminDataService.listUsers(request));
    }

    /** 分页查看帖子数据。 */
    @GetMapping("/posts/list")
    public Result<PageVO<AdminPostVO>> listPosts(@Valid @ModelAttribute AdminPageQueryRequest request) {
        return Result.success(adminDataService.listPosts(request));
    }

    /** 分页查看店铺数据。 */
    @GetMapping("/shops/list")
    public Result<PageVO<AdminShopVO>> listShops(@Valid @ModelAttribute AdminPageQueryRequest request) {
        return Result.success(adminDataService.listShops(request));
    }

    /** 分页查看商品数据。 */
    @GetMapping("/products/list")
    public Result<PageVO<AdminProductVO>> listProducts(@Valid @ModelAttribute AdminPageQueryRequest request) {
        return Result.success(adminDataService.listProducts(request));
    }
}
