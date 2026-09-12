package com.campushub.admin.controller;

import com.campushub.admin.service.MenuService;
import com.campushub.admin.vo.MenuVO;
import com.campushub.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 菜单接口：为前端动态路由提供数据。 */
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /** 查询当前登录用户按角色授权的菜单树（任何登录用户可访问，返回自己的菜单）。 */
    @GetMapping("/my")
    public Result<List<MenuVO>> my() {
        return Result.success(menuService.myMenus());
    }
}
