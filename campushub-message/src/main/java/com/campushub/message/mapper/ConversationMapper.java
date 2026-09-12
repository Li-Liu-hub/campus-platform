package com.campushub.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 会话表数据访问接口。 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 功能：查询当前用户参与的会话列表，一次组装对方信息、最后一条消息与未读数。
     *
     * <p>单条 SQL 内完成三件事：取对方用户与昵称（参与双列按当前用户取反）、
     * 取每会话最后一条消息（消息 ID 单调递增，MAX(id) 即最后一条）、
     * 按会话聚合当前用户的未读数；游标分页按 (create_time, conversation_id) 倒序。
     *
     * @param userId 当前登录用户 ID
     * @param cursorTime 上一页末行的创建时间，首页为 null
     * @param cursorId 上一页末行的会话 ID，首页为 null
     * @param pageSize 页大小
     * @return 会话列表行，按创建时间倒序
     */
    List<ConversationListRow> selectPageWithPeer(@Param("userId") Long userId,
                                                 @Param("cursorTime") LocalDateTime cursorTime,
                                                 @Param("cursorId") Long cursorId,
                                                 @Param("pageSize") int pageSize);

    /**
     * 功能：查询未删除用户的昵称，用户不存在或已删除时返回 null。
     *
     * <p>消息模块不依赖用户模块的代码，直接以 SQL 访问 ch_user（与帖子模块
     * 查询发帖人昵称同一模式），避免业务模块之间的编译期依赖。
     *
     * @param userId 用户 ID
     * @return 用户昵称；用户不存在或已删除时返回 null
     */
    String selectActiveUserNickname(@Param("userId") Long userId);
}
