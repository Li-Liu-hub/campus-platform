package com.campushub.admin.service.impl;

import com.campushub.admin.constant.RoleCodes;
import com.campushub.admin.dto.AdminPageQueryRequest;
import com.campushub.admin.mapper.AdminDataMapper;
import com.campushub.admin.service.AdminDataService;
import com.campushub.admin.vo.AdminPostVO;
import com.campushub.admin.vo.AdminProductVO;
import com.campushub.admin.vo.AdminShopVO;
import com.campushub.admin.vo.AdminUserVO;
import com.campushub.admin.vo.PageVO;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * 管理端数据查看业务实现。
 *
 * <p>权限模型：admin 模块的接口统一在服务层做管理员校验（查当前用户角色编码），
 * 校验失败返回 403；列表统一游标分页，排序与游标列一致（(create_time, 主键) 倒序）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDataServiceImpl implements AdminDataService {

    private final AdminDataMapper adminDataMapper;

    private final AuthenticationService authenticationService;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 分页查看用户数据，含角色编码供管理端核对权限归属。 */
    @Override
    public PageVO<AdminUserVO> listUsers(AdminPageQueryRequest request) {
        requireAdmin();
        int pageSize = resolvePageSize(request.pageSize());
        List<AdminUserVO> rows = adminDataMapper.selectUserPage(
                request.cursorTime(), request.cursorId(), pageSize);
        return toPage(rows, pageSize, AdminUserVO::getCreateTime, AdminUserVO::getUserId);
    }

    /** 分页查看帖子数据，发帖人昵称由 SQL JOIN 组装。 */
    @Override
    public PageVO<AdminPostVO> listPosts(AdminPageQueryRequest request) {
        requireAdmin();
        int pageSize = resolvePageSize(request.pageSize());
        List<AdminPostVO> rows = adminDataMapper.selectPostPage(
                request.cursorTime(), request.cursorId(), pageSize);
        return toPage(rows, pageSize, AdminPostVO::getCreateTime, AdminPostVO::getPostId);
    }

    /** 分页查看店铺数据，店主昵称由 SQL JOIN 组装。 */
    @Override
    public PageVO<AdminShopVO> listShops(AdminPageQueryRequest request) {
        requireAdmin();
        int pageSize = resolvePageSize(request.pageSize());
        List<AdminShopVO> rows = adminDataMapper.selectShopPage(
                request.cursorTime(), request.cursorId(), pageSize);
        return toPage(rows, pageSize, AdminShopVO::getCreateTime, AdminShopVO::getShopId);
    }

    /** 分页查看商品数据，所属店铺名称由 SQL JOIN 组装。 */
    @Override
    public PageVO<AdminProductVO> listProducts(AdminPageQueryRequest request) {
        requireAdmin();
        int pageSize = resolvePageSize(request.pageSize());
        List<AdminProductVO> rows = adminDataMapper.selectProductPage(
                request.cursorTime(), request.cursorId(), pageSize);
        return toPage(rows, pageSize, AdminProductVO::getCreateTime, AdminProductVO::getProductId);
    }

    /**
     * 功能：通用分页组装：取满一页说明可能还有更多数据，以末行 (create_time, 主键) 作为下一页游标。
     *
     * @param rows 本页数据行
     * @param pageSize 页大小
     * @param timeGetter 行对象的创建时间取值函数
     * @param idGetter 行对象的主键取值函数
     * @return 统一分页返回对象
     */
    private <T> PageVO<T> toPage(List<T> rows, int pageSize,
                                 Function<T, LocalDateTime> timeGetter, Function<T, Long> idGetter) {
        if (rows.size() < pageSize) {
            return new PageVO<>(rows, false, null, null);
        }
        T lastRow = rows.get(rows.size() - 1);
        return new PageVO<>(rows, true, timeGetter.apply(lastRow), idGetter.apply(lastRow));
    }

    /**
     * 校验当前登录用户是管理员，否则抛出 403 业务异常。
     *
     * <p>角色取自 ch_user.user_role（与 ch_role.role_code 对齐）；用户不存在或已删除时
     * 查询结果为 null，同样按非管理员处理，不泄露账号状态。
     */
    private void requireAdmin() {
        Long userId = requireCurrentUserId();
        String roleCode = adminDataMapper.selectUserRole(userId);
        if (!RoleCodes.ADMIN.equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
    }

    /** 页大小缺省 20，并限制在 1 到 100 之间。 */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
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
