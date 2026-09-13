package com.campushub.user.service;

import com.campushub.user.dto.FollowQueryRequest;
import com.campushub.user.vo.FollowUserVO;
import com.campushub.user.vo.PageVO;

/** 用户关注业务接口。 */
public interface FollowService {

    /**
     * 功能：关注目标用户。
     *
     * <p>重复关注幂等成功（不重复发通知）；成功后给被关注者创建站内通知。
     *
     * @param targetUserId 被关注用户 ID
     */
    void follow(Long targetUserId);

    /** 取消关注：未关注时幂等成功。 */
    void unfollow(Long targetUserId);

    /** 我关注的用户列表，按关注时间倒序游标分页。 */
    PageVO<FollowUserVO> following(FollowQueryRequest request);

    /** 关注我的用户列表（粉丝），按关注时间倒序游标分页。 */
    PageVO<FollowUserVO> followers(FollowQueryRequest request);
}
