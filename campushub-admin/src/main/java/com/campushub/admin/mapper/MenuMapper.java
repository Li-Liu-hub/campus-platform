package com.campushub.admin.mapper;

import com.campushub.admin.vo.MenuVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 菜单表数据访问接口。 */
@Mapper
public interface MenuMapper {

    /**
     * 功能：按角色编码查询已授权且未删除的菜单，按「父 + 排序」升序返回扁平列表，由服务层组装成树。
     *
     * @param roleCode 角色编码，与 ch_role.role_code 对齐
     * @return 菜单扁平列表；角色无授权菜单时返回空列表
     */
    List<MenuVO> selectMenusByRoleCode(@Param("roleCode") String roleCode);
}
