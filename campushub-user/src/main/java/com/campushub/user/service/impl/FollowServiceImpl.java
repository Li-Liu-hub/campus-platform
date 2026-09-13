package com.campushub.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.notification.constant.NotificationTypes;
import com.campushub.notification.service.NotificationService;
import com.campushub.user.dto.FollowQueryRequest;
import com.campushub.user.entity.Follow;
import com.campushub.user.entity.User;
import com.campushub.user.mapper.FollowMapper;
import com.campushub.user.mapper.UserMapper;
import com.campushub.user.service.FollowService;
import com.campushub.user.vo.FollowUserVO;
import com.campushub.user.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/** 用户关注业务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;

    private final UserMapper userMapper;

    private final AuthenticationService authenticationService;

    private final NotificationService notificationService;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 功能：关注目标用户，重复关注幂等成功。
     *
     * <p>顺序刻意为「先落关注关系、再发通知」：通知失败只记日志、不反噬关注主流程
     * （关注是事实，通知是提醒）；确认落库成功才发通知，避免"通知了但其实没关注"。
     */
    @Override
    public void follow(Long targetUserId) {
        Long userId = requireCurrentUserId();
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能关注自己");
        }
        // 目标用户必须存在且未注销（@TableLogic 自动过滤软删）
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "目标用户不存在");
        }
        Follow follow = new Follow();
        follow.setFollowSentUserId(userId);
        follow.setFollowReceiveUserId(targetUserId);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException exception) {
            // 已关注：幂等成功，不重复发通知
            return;
        }
        try {
            User operator = userMapper.selectById(userId);
            String operatorName = operator == null ? "有人" : operator.getUserNickname();
            notificationService.create(targetUserId, NotificationTypes.FOLLOW, operatorName + " 关注了你");
        } catch (Exception exception) {
            log.warn("关注通知创建失败，followerId={}，targetUserId={}", userId, targetUserId, exception);
        }
    }

    /** 取消关注：删除幂等，未关注时影响 0 行同样返回成功。 */
    @Override
    public void unfollow(Long targetUserId) {
        Long userId = requireCurrentUserId();
        followMapper.delete(Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getFollowSentUserId, userId)
                .eq(Follow::getFollowReceiveUserId, targetUserId));
    }

    /** 我关注的用户列表，按关注时间倒序游标分页。 */
    @Override
    public PageVO<FollowUserVO> following(FollowQueryRequest request) {
        Long userId = requireCurrentUserId();
        int pageSize = resolvePageSize(request.pageSize());
        List<FollowUserVO> rows = followMapper.selectFollowing(
                userId, request.cursorTime(), request.cursorId(), pageSize);
        return toPage(rows, pageSize);
    }

    /** 关注我的用户列表（粉丝），按关注时间倒序游标分页。 */
    @Override
    public PageVO<FollowUserVO> followers(FollowQueryRequest request) {
        Long userId = requireCurrentUserId();
        int pageSize = resolvePageSize(request.pageSize());
        List<FollowUserVO> rows = followMapper.selectFollowers(
                userId, request.cursorTime(), request.cursorId(), pageSize);
        return toPage(rows, pageSize);
    }

    /** 分页组装：取满一页时以末行 (followTime, followId) 作为下一页游标。 */
    private PageVO<FollowUserVO> toPage(List<FollowUserVO> rows, int pageSize) {
        if (rows.size() < pageSize) {
            return new PageVO<>(rows, false, null, null);
        }
        FollowUserVO lastRow = rows.get(rows.size() - 1);
        return new PageVO<>(rows, true, lastRow.getFollowTime(), lastRow.getFollowId());
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
