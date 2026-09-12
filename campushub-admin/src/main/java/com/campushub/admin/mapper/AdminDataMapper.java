package com.campushub.admin.mapper;

import com.campushub.admin.vo.AdminPostVO;
import com.campushub.admin.vo.AdminProductVO;
import com.campushub.admin.vo.AdminShopVO;
import com.campushub.admin.vo.AdminUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端数据查看访问接口：直接以 SQL 访问各业务表（与消息模块查询用户表同一模式），
 * 不依赖任何业务模块的代码，管理端查询与业务接口的演进互不牵连。
 *
 * <p>所有列表只查未删除数据（软删过滤写在 SQL 内，手写 SQL 不受 @TableLogic 影响），
 * 统一按 (create_time, 主键) 倒序游标分页。
 */
@Mapper
public interface AdminDataMapper {

    /**
     * 功能：分页查询未删除用户，按注册时间倒序。
     *
     * @param cursorTime 上一页末行的创建时间，首页为 null
     * @param cursorId 上一页末行的用户 ID，首页为 null
     * @param pageSize 页大小
     * @return 用户列表行
     */
    List<AdminUserVO> selectUserPage(@Param("cursorTime") LocalDateTime cursorTime,
                                     @Param("cursorId") Long cursorId,
                                     @Param("pageSize") int pageSize);

    /**
     * 功能：分页查询未删除帖子（含发帖人昵称），按发布时间倒序。
     *
     * @param cursorTime 上一页末行的创建时间，首页为 null
     * @param cursorId 上一页末行的帖子 ID，首页为 null
     * @param pageSize 页大小
     * @return 帖子列表行
     */
    List<AdminPostVO> selectPostPage(@Param("cursorTime") LocalDateTime cursorTime,
                                     @Param("cursorId") Long cursorId,
                                     @Param("pageSize") int pageSize);

    /**
     * 功能：分页查询未删除店铺（含店主昵称），按创建时间倒序。
     *
     * @param cursorTime 上一页末行的创建时间，首页为 null
     * @param cursorId 上一页末行的店铺 ID，首页为 null
     * @param pageSize 页大小
     * @return 店铺列表行
     */
    List<AdminShopVO> selectShopPage(@Param("cursorTime") LocalDateTime cursorTime,
                                     @Param("cursorId") Long cursorId,
                                     @Param("pageSize") int pageSize);

    /**
     * 功能：分页查询未删除商品（含所属店铺名称），按创建时间倒序。
     *
     * @param cursorTime 上一页末行的创建时间，首页为 null
     * @param cursorId 上一页末行的商品 ID，首页为 null
     * @param pageSize 页大小
     * @return 商品列表行
     */
    List<AdminProductVO> selectProductPage(@Param("cursorTime") LocalDateTime cursorTime,
                                           @Param("cursorId") Long cursorId,
                                           @Param("pageSize") int pageSize);

    /**
     * 功能：查询未删除用户的角色编码，供管理端接口做管理员校验。
     *
     * @param userId 用户 ID
     * @return 角色编码；用户不存在或已删除时返回 null
     */
    String selectUserRole(@Param("userId") Long userId);
}
