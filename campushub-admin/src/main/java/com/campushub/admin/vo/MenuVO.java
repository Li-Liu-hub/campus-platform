package com.campushub.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点，前端动态路由的直接数据源。
 *
 * <p>基础字段由 SQL 直接映射（@Data 类），children 由服务层按 parentId 组装；
 * 字段命名即前端契约：path 注册路由、component 映射组件、visible 控制菜单显隐。
 */
@Data
public class MenuVO {

    /** 菜单 ID。 */
    private Long menuId;

    /** 父菜单 ID，0 表示根节点。 */
    private Long menuParentId;

    /** 菜单名称（路由标题）。 */
    private String menuName;

    /** 路由路径。 */
    private String menuPath;

    /** 前端组件路径。 */
    private String menuComponent;

    /** 菜单图标。 */
    private String menuIcon;

    /** 同级排序。 */
    private Integer menuSort;

    /** 是否在菜单中显示：1 显示，0 隐藏。 */
    private Integer menuVisible;

    /** 权限标识，为按钮级权限校验预留。 */
    private String menuPermission;

    /** 子菜单，按组装顺序排列。 */
    private List<MenuVO> children = new ArrayList<>();
}
