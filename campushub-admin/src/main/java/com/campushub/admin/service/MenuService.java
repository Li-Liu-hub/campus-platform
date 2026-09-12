package com.campushub.admin.service;

import com.campushub.admin.vo.MenuVO;

import java.util.List;

/** 菜单业务接口：为前端动态路由提供按角色授权的菜单树。 */
public interface MenuService {

    /**
     * 功能：查询当前登录用户按角色授权的菜单树，作为前端动态路由数据源。
     *
     * <p>任何登录用户都可访问（返回的是自己的菜单）：角色编码取自 ch_user.user_role，
     * 无授权菜单时返回空列表而非报错——前端渲染空路由即可。
     *
     * @return 菜单树根节点列表，同级按 menu_sort 升序
     */
    List<MenuVO> myMenus();
}
