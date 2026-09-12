package com.campushub.admin.service.impl;

import com.campushub.admin.mapper.AdminDataMapper;
import com.campushub.admin.mapper.MenuMapper;
import com.campushub.admin.service.MenuService;
import com.campushub.admin.vo.MenuVO;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 菜单业务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    private final AdminDataMapper adminDataMapper;

    private final AuthenticationService authenticationService;

    /** 根节点的父菜单 ID。 */
    private static final long ROOT_PARENT_ID = 0L;

    /**
     * 功能：查询当前登录用户按角色授权的菜单树。
     *
     * <p>查询链路：当前用户 → ch_user.user_role 取角色编码 → 授权菜单扁平列表（已按父+排序）
     * → 按 parentId 分组一次组装成树。用户角色查不到（账号异常）时返回空列表。
     */
    @Override
    public List<MenuVO> myMenus() {
        Long userId = requireCurrentUserId();
        String roleCode = adminDataMapper.selectUserRole(userId);
        if (roleCode == null) {
            return List.of();
        }
        List<MenuVO> flatMenus = menuMapper.selectMenusByRoleCode(roleCode);
        return buildTree(flatMenus);
    }

    /**
     * 功能：把「父 + 排序」有序的扁平菜单组装成树。
     *
     * <p>按 parentId 分组时使用 LinkedHashMap 保序：SQL 已按 (parent_id, sort) 排好，
     * 分组后组内顺序与查询顺序一致，无需二次排序。
     *
     * @param flatMenus 扁平菜单列表，必须按 (menu_parent_id, menu_sort) 升序
     * @return 根节点菜单树
     */
    private List<MenuVO> buildTree(List<MenuVO> flatMenus) {
        Map<Long, List<MenuVO>> childrenByParent = flatMenus.stream()
                .collect(Collectors.groupingBy(MenuVO::getMenuParentId,
                        LinkedHashMap::new, Collectors.toList()));
        return attachChildren(ROOT_PARENT_ID, childrenByParent);
    }

    /** 递归挂载子节点：取该父节点下的子列表，逐个把自身子树挂到 children 上。 */
    private List<MenuVO> attachChildren(Long parentId, Map<Long, List<MenuVO>> childrenByParent) {
        List<MenuVO> children = childrenByParent.getOrDefault(parentId, List.of());
        for (MenuVO child : children) {
            child.setChildren(attachChildren(child.getMenuId(), childrenByParent));
        }
        return children;
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }
}
