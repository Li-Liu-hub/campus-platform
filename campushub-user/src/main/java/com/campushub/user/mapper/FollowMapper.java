package com.campushub.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.user.entity.Follow;
import com.campushub.user.vo.FollowUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 用户关注表数据访问接口。 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 功能：查询我关注的用户列表（含对方昵称与签名），按 (关注时间, 关注 ID) 倒序游标分页。
     *
     * @param userId 当前登录用户 ID
     * @param cursorTime 上一页末行的关注时间，首页为 null
     * @param cursorId 上一页末行的关注关系 ID，首页为 null
     * @param pageSize 页大小
     * @return 关注用户列表
     */
    List<FollowUserVO> selectFollowing(@Param("userId") Long userId,
                                       @Param("cursorTime") LocalDateTime cursorTime,
                                       @Param("cursorId") Long cursorId,
                                       @Param("pageSize") int pageSize);

    /**
     * 功能：查询关注我的用户列表（粉丝，含对方昵称与签名），按 (关注时间, 关注 ID) 倒序游标分页。
     *
     * @param userId 当前登录用户 ID
     * @param cursorTime 上一页末行的关注时间，首页为 null
     * @param cursorId 上一页末行的关注关系 ID，首页为 null
     * @param pageSize 页大小
     * @return 粉丝用户列表
     */
    List<FollowUserVO> selectFollowers(@Param("userId") Long userId,
                                       @Param("cursorTime") LocalDateTime cursorTime,
                                       @Param("cursorId") Long cursorId,
                                       @Param("pageSize") int pageSize);
}
